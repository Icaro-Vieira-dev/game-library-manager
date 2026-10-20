package com.gamelibrary.gamelibrarymanager.service;

import com.gamelibrary.gamelibrarymanager.dto.steam.DetalhesSteamResultadoDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamConquistaDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamConquistasResponseDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamGameDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamLojaAppDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamResponseDTO;
import com.gamelibrary.gamelibrarymanager.exception.SteamImportacaoException;
import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.repository.JogoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final RestTemplate restTemplate;
    private final JogoRepository jogoRepository;
    private final CategoriaService categoriaService;
    // vêm do application.properties, que por sua vez lê o arquivo .env
    private final String apiKey;
    private final String steamIdPadrao;

    @Autowired
    public SteamService(RestTemplate restTemplate, JogoRepository jogoRepository, CategoriaService categoriaService,
                        @Value("${steam.api-key}") String apiKey,
                        @Value("${steam.steam-id}") String steamIdPadrao) {
        this.restTemplate = restTemplate;
        this.jogoRepository = jogoRepository;
        this.categoriaService = categoriaService;
        this.apiKey = apiKey;
        this.steamIdPadrao = steamIdPadrao;
    }

    // steamId é opcional: se vier null ou vazio, usa o STEAM_ID do .env.
    // Retorna só os jogos novos; os que já foram importados antes (mesmo appid) são ignorados.
    @Transactional
    public List<Jogo> importarJogos(String steamId){
        validarApiKey();
        String steamIdUsado = resolverSteamId(steamId);

        // monta a URL com os parâmetros já codificados (ex.: espaços e acentos não quebram a URL)
        URI url = UriComponentsBuilder.fromUriString(URL_JOGOS_DO_USUARIO)
                .queryParam("key", apiKey)
                .queryParam("steamid", steamIdUsado)
                .queryParam("include_appinfo", true)
                .build()
                .toUri();

        SteamResponseDTO responseDTO;

        try {
            responseDTO = restTemplate.getForObject(url, SteamResponseDTO.class);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            // a Steam responde 401/403 quando a chave é inválida
            throw new SteamImportacaoException("A Steam recusou a API key. Confira o valor de STEAM_API_KEY no arquivo .env.");
        }

        // perfil privado ou steamId inexistente: a Steam responde sem a lista "games"
        if (responseDTO == null || responseDTO.getResponse() == null || responseDTO.getResponse().getGames() == null) {
            throw new SteamImportacaoException("A Steam não retornou jogos para o steamId " + steamIdUsado
                    + ". Verifique se o ID está correto e se os detalhes de jogo do perfil estão públicos.");
        }

        Set<Long> appIdsJaImportados = new HashSet<>(jogoRepository.findAllSteamAppIds());
        List<Jogo> jogosNovos = new ArrayList<>();

        for (SteamGameDTO steamGameDTO: responseDTO.getResponse().getGames()){
            long appId = steamGameDTO.getAppid();
            // add() devolve false se o appid já estava no Set
            if (!appIdsJaImportados.add(appId)) {
                continue;
            }
            Jogo jogo = new Jogo();
            jogo.setSteamAppId(appId);
            jogo.setNomeJogo(steamGameDTO.getName());
            jogo.setHorasJogadas(steamGameDTO.getPlaytime_forever() / 60.0);
            jogo.setStatusJogo(definirStatus(steamGameDTO, Instant.now()));

            jogosNovos.add(jogo);
        }
        return jogoRepository.saveAll(jogosNovos);
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
            responseDTO = restTemplate.getForObject(url, SteamConquistasResponseDTO.class);
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
        if (responseDTO == null || responseDTO.getPlayerstats() == null
                || responseDTO.getPlayerstats().getAchievements() == null
                || responseDTO.getPlayerstats().getAchievements().isEmpty()) {
            throw new SteamImportacaoException("Este jogo não tem conquistas na Steam (appid " + steamAppId + ").");
        }
        return responseDTO.getPlayerstats().getAchievements();
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
        Map<String, SteamLojaAppDTO> resposta = restTemplate.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, SteamLojaAppDTO>>() {}).getBody();

        SteamLojaAppDTO app = (resposta == null) ? null : resposta.get(String.valueOf(steamAppId));
        if (app == null || !app.isSuccess() || app.getData() == null) {
            return null;
        }
        return app.getData();
    }

    // preenche só o que está vazio
    private void aplicarDadosLoja(Jogo jogo, SteamLojaAppDTO.Dados dados) {
        List<SteamLojaAppDTO.Genero> generos = (dados.getGenres() == null) ? List.of() : dados.getGenres();

        // cada gênero da Steam vira uma categoria (reaproveita se já existir) ligada ao jogo
        for (SteamLojaAppDTO.Genero genero : generos) {
            if (genero.getDescription() != null && !genero.getDescription().isBlank()) {
                jogo.adicionarCategoria(categoriaService.buscarOuCriarPorNome(genero.getDescription()));
            }
        }
        // o primeiro gênero é o principal
        if (estaVazio(jogo.getGeneroJogo()) && !generos.isEmpty()) {
            jogo.setGeneroJogo(generos.get(0).getDescription());
        }
        if (estaVazio(jogo.getDesenvolvedoraJogo()) && dados.getDevelopers() != null && !dados.getDevelopers().isEmpty()) {
            jogo.setDesenvolvedoraJogo(String.join(", ", dados.getDevelopers()));
        }
        if (jogo.getAnoLancamentoJogo() == 0 && dados.getRelease_date() != null) {
            jogo.setAnoLancamentoJogo(extrairAno(dados.getRelease_date().getDate()));
        }
        if (estaVazio(jogo.getPlataformaJogo())) {
            jogo.setPlataformaJogo("PC");
        }
        if (estaVazio(jogo.getImagemBanner()) && !estaVazio(dados.getHeader_image())) {
            jogo.setImagemBanner(dados.getHeader_image());
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
    static GameStatus definirStatus(SteamGameDTO steamGameDTO, Instant agora) {
        if (steamGameDTO.getPlaytime_forever() == 0) {
            return GameStatus.PLANEJADO;
        }

        // playtime_2weeks é o sinal principal; rtime_last_played cobre o caso de ter aberto o jogo
        // recentemente sem a Steam ter contabilizado minutos nas 2 semanas
        long ultimaVez = steamGameDTO.getRtime_last_played();
        boolean jogouRecentemente = steamGameDTO.getPlaytime_2weeks() > 0
                || (ultimaVez > 0 && Instant.ofEpochSecond(ultimaVez).isAfter(agora.minus(JANELA_JOGANDO)));

        if (jogouRecentemente) {
            return GameStatus.JOGANDO;
        }
        return steamGameDTO.getPlaytime_forever() >= MINUTOS_PARA_FINALIZADO
                ? GameStatus.FINALIZADO
                : GameStatus.ABANDONADO;
    }

}
