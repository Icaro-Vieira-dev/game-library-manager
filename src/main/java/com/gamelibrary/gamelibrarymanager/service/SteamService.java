package com.gamelibrary.gamelibrarymanager.service;

import com.gamelibrary.gamelibrarymanager.dto.JogoResponseDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.DetalhesSteamResultadoDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.ImportacaoSteamResultadoDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamConquistaDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamConquistasResponseDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamJogoDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamLojaAppDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamBibliotecaResponseDTO;
import com.gamelibrary.gamelibrarymanager.exception.SteamImportacaoException;
import com.gamelibrary.gamelibrarymanager.model.StatusJogo;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.model.Usuario;
import com.gamelibrary.gamelibrarymanager.repository.JogoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SteamService {
    private static final String URL_JOGOS_DO_USUARIO = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/";
    private static final String URL_CONQUISTAS_DO_USUARIO = "https://api.steampowered.com/ISteamUserStats/GetPlayerAchievements/v1/";
    // API da LOJA (não oficial, sem API key): aceita só um appid por consulta
    private static final String URL_LOJA_DETALHES = "https://store.steampowered.com/api/appdetails";
    // ~200 consultas a cada 5 minutos = 1 a cada 1,5 s
    private static final Duration PAUSA_ENTRE_CONSULTAS_LOJA = Duration.ofMillis(1500);
    // jogou dentro desse período = JOGANDO; mesmo período que o playtime_2weeks da Steam
    private static final Duration JANELA_JOGANDO = Duration.ofDays(14);
    // não jogou recentemente e tem pelo menos isso de tempo = provavelmente zerou (FINALIZADO)
    private static final int MINUTOS_PARA_FINALIZADO = 10 * 60;


    // a Steam esconde jogos grátis por padrão; com true, vêm os que você já jogou
    private static final boolean INCLUIR_JOGOS_GRATUITOS = true;
    // jogos ganhos de graça (promoções, brindes, testes)
    private static final boolean INCLUIR_INSCRICAO_GRATUITA = true;
    // false = não pular os apps "não verificados" que a Steam esconde por padrão
    private static final boolean PULAR_APP_NAO_VERIFICADO = false;


    // mensagens do 422 quando o usuário ainda não informou o Steam ID (ou o removeu)
    static final String SEM_STEAM_ID = "Cadastre o seu Steam ID antes de importar (PATCH /usuarios/me).";
    static final String SEM_STEAM_ID_CONQUISTAS =
            "Cadastre o seu Steam ID (PATCH /usuarios/me) para buscar as conquistas na Steam.";

    private final RestClient restClient;
    private final JogoRepository jogoRepository;
    private final CategoriaService categoriaService;
    // a chave da APLICAÇÃO (vem do application.properties, que lê o .env). Nunca pedimos nem guardamos
    // a chave da Steam do usuário: dele, só usamos o Steam ID público (Usuario.steamId).
    private final String apiKey;

    @Autowired
    public SteamService(RestClient restClient, JogoRepository jogoRepository, CategoriaService categoriaService,
                        @Value("${steam.api-key}") String apiKey) {
        this.restClient = restClient;
        this.jogoRepository = jogoRepository;
        this.categoriaService = categoriaService;
        this.apiKey = apiKey;
    }

    // Importa a biblioteca da Steam do USUÁRIO LOGADO, usando o Steam ID dele (sem Steam ID -> 422).
    // Jogos novos são cadastrados com ele como dono; nos que ele já tem (mesmo appid), só as horas
    // jogadas são atualizadas. Os jogos dos outros usuários não são tocados.
    @Transactional
    public ImportacaoSteamResultadoDTO importarJogos(Usuario usuario){
        validarApiKey();
        String steamIdUsado = steamIdObrigatorio(usuario.getSteamId(), SEM_STEAM_ID);

        // monta a URL com os parâmetros já codificados (ex.: espaços e acentos não quebram a URL)
        URI url = UriComponentsBuilder.fromUriString(URL_JOGOS_DO_USUARIO)
                .queryParam("key", apiKey)
                .queryParam("steamid", steamIdUsado)
                .queryParam("include_appinfo", true)
                .queryParam("include_played_free_games", INCLUIR_JOGOS_GRATUITOS)
                .queryParam("include_free_sub", INCLUIR_INSCRICAO_GRATUITA)
                .queryParam("skip_unvetted_apps", PULAR_APP_NAO_VERIFICADO)
                .build()
                .toUri();

        SteamBibliotecaResponseDTO responseDTO;

        try {
            responseDTO = restClient.get().uri(url).retrieve().body(SteamBibliotecaResponseDTO.class);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            // a Steam responde 401/403 quando a chave é inválida
            throw new SteamImportacaoException("A Steam recusou a API key. Confira o valor de STEAM_API_KEY no arquivo .env.");
        }

        // perfil privado ou steamId inexistente: a Steam responde sem a lista "games"
        if (responseDTO == null || responseDTO.response() == null || responseDTO.response().jogos() == null) {
            throw new SteamImportacaoException("A Steam não retornou jogos para o steamId " + steamIdUsado
                    + ". Verifique se o ID está correto e se os detalhes de jogo do perfil estão públicos.");
        }

        // os jogos da Steam que ESSE usuário já tem no banco, indexados pelo appid
        Map<Long, Jogo> jaImportados = new HashMap<>();
        for (Jogo jogo : jogoRepository.findByUsuarioAndSteamAppIdIsNotNull(usuario)) {
            jaImportados.put(jogo.getSteamAppId(), jogo);
        }

        ResultadoMescla resultado = mesclarBiblioteca(responseDTO.response().jogos(), jaImportados, Instant.now());
        // os jogos novos nascem com o dono logado (a mescla é static e não conhece usuário; sem dono,
        // o banco recusaria o INSERT, porque usuario_id é NOT NULL)
        resultado.novos().forEach(jogo -> jogo.setUsuario(usuario));
        List<Jogo> novosSalvos = jogoRepository.saveAll(resultado.novos());
        jogoRepository.saveAll(resultado.atualizados());

        return new ImportacaoSteamResultadoDTO(
                novosSalvos.size(),
                resultado.atualizados().size(),
                novosSalvos.stream().map(JogoResponseDTO::deJogo).toList());
    }

    // O que a importação vai fazer: jogos para cadastrar e jogos que tiveram as horas atualizadas.
    record ResultadoMescla(List<Jogo> novos, List<Jogo> atualizados) {
    }

    // Compara a biblioteca da Steam com o que já está no banco. Não acessa banco nem rede, por isso
    // dá para testar sozinho (veja SteamServiceMesclaTest).
    //   appid novo                         -> cria o jogo (com o status calculado pelas horas)
    //   appid que já existe, horas mudaram -> atualiza SÓ as horas (status, nota etc. ficam como estão,
    //                                         para não desfazer o que você ajustou à mão)
    //   appid que já existe, horas iguais  -> não faz nada
    static ResultadoMescla mesclarBiblioteca(List<SteamJogoDTO> daSteam, Map<Long, Jogo> jaImportados, Instant agora) {
        List<Jogo> novos = new ArrayList<>();
        List<Jogo> atualizados = new ArrayList<>();

        for (SteamJogoDTO steamJogo : daSteam) {
            long appId = steamJogo.appid();
            double horas = steamJogo.minutosJogados() / 60.0;
            Jogo existente = jaImportados.get(appId);

            if (existente == null) {
                Jogo jogo = new Jogo();
                jogo.setSteamAppId(appId);
                jogo.setNomeJogo(steamJogo.name());
                jogo.setHorasJogadas(horas);
                jogo.setStatusJogo(definirStatus(steamJogo, agora));
                novos.add(jogo);
                // registra no mapa: se o mesmo appid vier repetido na resposta, não cria duas vezes
                jaImportados.put(appId, jogo);
            } else if (Math.abs(existente.getHorasJogadas() - horas) > 0.001) {
                // compara com uma margem pequena: números com vírgula (double) podem ter arredondamento
                existente.setHorasJogadas(horas);
                atualizados.add(existente);
            }
        }
        return new ResultadoMescla(novos, atualizados);
    }

    // Busca na Steam todas as conquistas de um jogo, marcando quais o DONO do jogo já desbloqueou
    // (steamIdDoDono = Usuario.steamId do dono). Lança SteamImportacaoException (422) se o dono não tiver
    // Steam ID, se o jogo não tiver conquistas ou se o perfil estiver privado.
    public List<SteamConquistaDTO> buscarConquistas(long steamAppId, String steamIdDoDono) {
        validarApiKey();
        String steamIdUsado = steamIdObrigatorio(steamIdDoDono, SEM_STEAM_ID_CONQUISTAS);

        URI url = UriComponentsBuilder.fromUriString(URL_CONQUISTAS_DO_USUARIO)
                .queryParam("key", apiKey)
                .queryParam("steamid", steamIdUsado)
                .queryParam("appid", steamAppId)
                .queryParam("l", "brazilian") // nomes e descrições em português, quando o jogo tiver tradução
                .build()
                .toUri();

        SteamConquistasResponseDTO responseDTO;
        try {
            responseDTO = restClient.get().uri(url).retrieve().body(SteamConquistasResponseDTO.class);
        } catch (HttpClientErrorException e) {
            // nesses erros a Steam explica o motivo no corpo da resposta, ex.: {"playerstats":{"error":"Requested app has no stats"}}
            String corpo = e.getResponseBodyAsString();
            if (corpo.contains("no stats")) {
                throw new SteamImportacaoException("Este jogo não tem conquistas na Steam (appid " + steamAppId + ").");
            }
            if (corpo.contains("not public")) {
                throw new SteamImportacaoException("Suas conquistas estão privadas. Na Steam, deixe os 'Detalhes de jogo' do perfil como públicos.");
            }
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new SteamImportacaoException("A Steam recusou a API key. Confira o valor de STEAM_API_KEY no arquivo .env.");
            }
            // só o status: o corpo cru da Steam não vai para o cliente (não sabemos o que vem nele)
            throw new SteamImportacaoException("A Steam respondeu com erro " + e.getStatusCode().value() + " ao buscar as conquistas.");
        }

        // alguns jogos têm "stats" mas nenhuma conquista: a lista vem vazia ou nem vem
        if (responseDTO == null || responseDTO.playerstats() == null
                || responseDTO.playerstats().conquistas() == null
                || responseDTO.playerstats().conquistas().isEmpty()) {
            throw new SteamImportacaoException("Este jogo não tem conquistas na Steam (appid " + steamAppId + ").");
        }
        return responseDTO.playerstats().conquistas();
    }

    // Completa gênero, categorias, desenvolvedora, ano e plataforma dos jogos importados da Steam
    // DO USUÁRIO LOGADO, consultando a LOJA da Steam (não precisa de API key). Processa até "limite" jogos
    // por chamada, com uma pausa entre eles para respeitar o limite da loja (~200 consultas a cada 5 minutos).
    // Só preenche campos vazios: o que você editou à mão não é sobrescrito.
    // As categorias criadas aqui são do usuário.
    @Transactional
    public DetalhesSteamResultadoDTO completarDetalhes(int limite, Usuario usuario) {
        List<Jogo> pendentes = jogoRepository.findByUsuarioAndSteamAppIdIsNotNullAndDetalhesLojaBuscadosIsNullOrderByIdJogoAsc(usuario, Limit.of(limite));

        int processados = 0;
        int atualizados = 0;
        int semDados = 0;
        String mensagem = "Concluído.";

        for (Jogo jogo : pendentes) {
            if (processados > 0) {
                pausar(PAUSA_ENTRE_CONSULTAS_LOJA);
            }

            SteamLojaAppDTO.Dados dados;
            try {
                dados = buscarDadosLoja(jogo.getSteamAppId());
            } catch (HttpClientErrorException.TooManyRequests e) {
                // a loja pediu para esperar: para aqui; o que já foi feito fica salvo
                mensagem = "A loja da Steam limitou as consultas (429). Espere uns 5 minutos e chame de novo para continuar.";
                break;
            }
            processados++;

            if (dados == null) {
                // jogo removido da loja: marca para não tentar de novo a cada chamada
                jogo.setDetalhesLojaBuscados(true);
                semDados++;
                continue;
            }

            aplicarDadosLoja(jogo, dados, usuario);
            jogo.setDetalhesLojaBuscados(true);
            atualizados++;
        }
        jogoRepository.saveAll(pendentes);

        long restantes = jogoRepository.countByUsuarioAndSteamAppIdIsNotNullAndDetalhesLojaBuscadosIsNull(usuario);
        if (restantes > 0 && mensagem.equals("Concluído.")) {
            mensagem = "Ainda faltam " + restantes + " jogo(s). Chame de novo para continuar.";
        }
        return new DetalhesSteamResultadoDTO(processados, atualizados, semDados, restantes, mensagem);
    }

    // devolve null se o jogo não existe mais na loja ({"success": false})
    private SteamLojaAppDTO.Dados buscarDadosLoja(long steamAppId) {
        URI url = UriComponentsBuilder.fromUriString(URL_LOJA_DETALHES)
                .queryParam("appids", steamAppId)
                .queryParam("l", "brazilian")
                .build()
                .toUri();

        // a resposta é um mapa { "<appid>": { ... } }, por isso o ParameterizedTypeReference
        Map<String, SteamLojaAppDTO> resposta = restClient.get().uri(url).retrieve()
                .body(new ParameterizedTypeReference<Map<String, SteamLojaAppDTO>>() {});

        SteamLojaAppDTO app = (resposta == null) ? null : resposta.get(String.valueOf(steamAppId));
        if (app == null || !app.success() || app.data() == null) {
            return null;
        }
        return app.data();
    }

    // preenche só o que está vazio
    private void aplicarDadosLoja(Jogo jogo, SteamLojaAppDTO.Dados dados, Usuario dono) {
        List<SteamLojaAppDTO.Genero> generos = (dados.generos() == null) ? List.of() : dados.generos();

        // cada gênero da Steam vira uma categoria DO DONO (reaproveita se ele já tiver) ligada ao jogo.
        // Jogo e categoria são do mesmo usuário: um vínculo nunca junta donos diferentes.
        for (SteamLojaAppDTO.Genero genero : generos) {
            if (genero.descricao() != null && !genero.descricao().isBlank()) {
                jogo.adicionarCategoria(categoriaService.buscarOuCriarPorNome(genero.descricao(), dono));
            }
        }
        // o primeiro gênero é o principal
        if (estaVazio(jogo.getGeneroJogo()) && !generos.isEmpty()) {
            jogo.setGeneroJogo(generos.get(0).descricao());
        }
        if (estaVazio(jogo.getDesenvolvedoraJogo()) && dados.desenvolvedoras() != null && !dados.desenvolvedoras().isEmpty()) {
            jogo.setDesenvolvedoraJogo(String.join(", ", dados.desenvolvedoras()));
        }
        if (jogo.getAnoLancamentoJogo() == 0 && dados.dataLancamento() != null) {
            jogo.setAnoLancamentoJogo(extrairAno(dados.dataLancamento().data()));
        }
        if (estaVazio(jogo.getPlataformaJogo())) {
            jogo.setPlataformaJogo("PC");
        }
        if (estaVazio(jogo.getImagemBanner()) && !estaVazio(dados.imagemBanner())) {
            jogo.setImagemBanner(dados.imagemBanner());
        }
    }

    // A data vem em texto e o formato muda com o idioma ("24/fev./2022", "Feb 24, 2022", "2022").
    // Em vez de interpretar a data, pega o primeiro número de 4 dígitos. Sem ano (ex.: "Em breve") -> 0.
    static int extrairAno(String data) {
        if (data == null) {
            return 0;
        }
        Matcher matcher = Pattern.compile("\\b(\\d{4})\\b").matcher(data);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static boolean estaVazio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static void pausar(Duration duracao) {
        try {
            Thread.sleep(duracao);
        } catch (InterruptedException e) {
            // boa prática: se a thread foi interrompida, restaurar o aviso de interrupção
            Thread.currentThread().interrupt();
        }
    }

    private void validarApiKey() {
        if (apiKey.isBlank()) {
            throw new SteamImportacaoException("API key da Steam não configurada. Preencha STEAM_API_KEY no arquivo .env e reinicie a aplicação.");
        }
    }

    // O Steam ID vem SEMPRE do usuário (Usuario.steamId, informado no cadastro ou no PATCH /usuarios/me),
    // nunca de parâmetro da requisição nem do .env. O formato já foi validado quando ele foi salvo.
    private static String steamIdObrigatorio(String steamIdDoUsuario, String mensagemSeFaltar) {
        if (steamIdDoUsuario == null || steamIdDoUsuario.isBlank()) {
            throw new SteamImportacaoException(mensagemSeFaltar);
        }
        return steamIdDoUsuario;
    }

    // Regra do status inicial de um jogo importado:
    //   nunca jogou                                           -> PLANEJADO
    //   jogou nas últimas 2 semanas                           -> JOGANDO
    //   não jogou nas últimas 2 semanas, 10h ou mais no total -> FINALIZADO (provavelmente zerou)
    //   não jogou nas últimas 2 semanas, menos de 10h         -> ABANDONADO
    // "agora" é um parâmetro (em vez de Instant.now() aqui dentro) para o teste poder fixar a data.
    static StatusJogo definirStatus(SteamJogoDTO steamJogo, Instant agora) {
        if (steamJogo.minutosJogados() == 0) {
            return StatusJogo.PLANEJADO;
        }

        // playtime_2weeks é o sinal principal; rtime_last_played cobre o caso de ter aberto o jogo
        // recentemente sem a Steam ter contabilizado minutos nas 2 semanas
        long ultimaVez = steamJogo.ultimaVezJogado();
        boolean jogouRecentemente = steamJogo.minutosUltimas2Semanas() > 0
                || (ultimaVez > 0 && Instant.ofEpochSecond(ultimaVez).isAfter(agora.minus(JANELA_JOGANDO)));

        if (jogouRecentemente) {
            return StatusJogo.JOGANDO;
        }
        return steamJogo.minutosJogados() >= MINUTOS_PARA_FINALIZADO
                ? StatusJogo.FINALIZADO
                : StatusJogo.ABANDONADO;
    }

}
