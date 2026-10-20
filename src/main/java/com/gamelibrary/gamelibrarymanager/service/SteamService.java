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

    private final RestClient restClient;
    private final JogoRepository jogoRepository;
    private final CategoriaService categoriaService;
    // vêm do application.properties, que por sua vez lê o arquivo .env
    private final String apiKey;
    private final String steamIdPadrao;

    @Autowired
    public SteamService(RestClient restClient, JogoRepository jogoRepository, CategoriaService categoriaService,
                        @Value("${steam.api-key}") String apiKey,
                        @Value("${steam.steam-id}") String steamIdPadrao) {
        this.restClient = restClient;
        this.jogoRepository = jogoRepository;
        this.categoriaService = categoriaService;
        this.apiKey = apiKey;
        this.steamIdPadrao = steamIdPadrao;
    }

    // steamId é opcional: se vier null ou vazio, usa o STEAM_ID do .env.
    // Jogos novos são cadastrados; nos que já existem (mesmo appid), só as horas jogadas são atualizadas.
    @Transactional
    public ImportacaoSteamResultadoDTO importarJogos(String steamId){
        validarApiKey();
        String steamIdUsado = resolverSteamId(steamId);

        // monta a URL com os parâmetros já codificados (ex.: espaços e acentos não quebram a URL)
        URI url = UriComponentsBuilder.fromUriString(URL_JOGOS_DO_USUARIO)
                .queryParam("key", apiKey)
                .queryParam("steamid", steamIdUsado)
                .queryParam("include_appinfo", true)
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

        // os jogos da Steam que já estão no banco, indexados pelo appid
        Map<Long, Jogo> jaImportados = new HashMap<>();
        for (Jogo jogo : jogoRepository.findBySteamAppIdIsNotNull()) {
            jaImportados.put(jogo.getSteamAppId(), jogo);
        }

        ResultadoMescla resultado = mesclarBiblioteca(responseDTO.response().jogos(), jaImportados, Instant.now());
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

    // Busca na Steam todas as conquistas de um jogo, marcando quais o usuário do STEAM_ID (.env) já desbloqueou.
    // Lança SteamImportacaoException (422) se o jogo não tiver conquistas ou o perfil estiver privado.
    public List<SteamConquistaDTO> buscarConquistas(long steamAppId) {
        validarApiKey();
        String steamIdUsado = resolverSteamId(null);

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
            throw new SteamImportacaoException("A Steam respondeu com erro " + e.getStatusCode().value() + " ao buscar as conquistas: " + corpo);
        }

        // alguns jogos têm "stats" mas nenhuma conquista: a lista vem vazia ou nem vem
        if (responseDTO == null || responseDTO.playerstats() == null
                || responseDTO.playerstats().conquistas() == null
                || responseDTO.playerstats().conquistas().isEmpty()) {
            throw new SteamImportacaoException("Este jogo não tem conquistas na Steam (appid " + steamAppId + ").");
        }
        return responseDTO.playerstats().conquistas();
    }

    // Completa gênero, categorias, desenvolvedora, ano e plataforma dos jogos importados da Steam,
    // consultando a LOJA da Steam (não precisa de API key). Processa até "limite" jogos por chamada,
    // com uma pausa entre eles para respeitar o limite da loja (~200 consultas a cada 5 minutos).
    // Só preenche campos vazios: o que você editou à mão não é sobrescrito.
    @Transactional
    public DetalhesSteamResultadoDTO completarDetalhes(int limite) {
        List<Jogo> pendentes = jogoRepository.findBySteamAppIdIsNotNullAndDetalhesLojaBuscadosIsNullOrderByIdJogoAsc(Limit.of(limite));

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

            aplicarDadosLoja(jogo, dados);
            jogo.setDetalhesLojaBuscados(true);
            atualizados++;
        }
        jogoRepository.saveAll(pendentes);

        long restantes = jogoRepository.countBySteamAppIdIsNotNullAndDetalhesLojaBuscadosIsNull();
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
    private void aplicarDadosLoja(Jogo jogo, SteamLojaAppDTO.Dados dados) {
        List<SteamLojaAppDTO.Genero> generos = (dados.generos() == null) ? List.of() : dados.generos();

        // cada gênero da Steam vira uma categoria (reaproveita se já existir) ligada ao jogo
        for (SteamLojaAppDTO.Genero genero : generos) {
            if (genero.descricao() != null && !genero.descricao().isBlank()) {
                jogo.adicionarCategoria(categoriaService.buscarOuCriarPorNome(genero.descricao()));
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

    // usa o steamId recebido ou, se vier null/vazio, o STEAM_ID do .env; valida o formato
    private String resolverSteamId(String steamId) {
        String steamIdUsado = (steamId == null || steamId.isBlank()) ? steamIdPadrao : steamId.strip();
        if (steamIdUsado.isBlank()) {
            throw new SteamImportacaoException("Nenhum steamId informado. Passe ?steamId=... na requisição ou preencha STEAM_ID no arquivo .env.");
        }
        // SteamID64 sempre tem 17 dígitos. Nome de perfil personalizado (ex.: "icaro") não funciona nessa API.
        if (!steamIdUsado.matches("\\d{17}")) {
            throw new SteamImportacaoException("steamId inválido: " + steamIdUsado
                    + ". Use o SteamID64, com 17 dígitos (começa com 7656...). Você encontra em https://steamid.io");
        }
        return steamIdUsado;
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
