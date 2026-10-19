package com.gamelibrary.gamelibrarymanager.service;

import com.gamelibrary.gamelibrarymanager.dto.steam.SteamConquistaDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamConquistasResponseDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamGameDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamResponseDTO;
import com.gamelibrary.gamelibrarymanager.exception.SteamImportacaoException;
import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.repository.JogoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Set;

@Service
public class SteamService {
    private static final String URL_JOGOS_DO_USUARIO = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/";
    private static final String URL_CONQUISTAS_DO_USUARIO = "https://api.steampowered.com/ISteamUserStats/GetPlayerAchievements/v1/";
    // jogou dentro desse período = JOGANDO; mesmo período que o playtime_2weeks da Steam
    private static final Duration JANELA_JOGANDO = Duration.ofDays(14);
    // não jogou recentemente e tem pelo menos isso de tempo = provavelmente zerou (FINALIZADO)
    private static final int MINUTOS_PARA_FINALIZADO = 10 * 60;

    private final RestTemplate restTemplate;
    private final JogoRepository jogoRepository;
    // vêm do application.properties, que por sua vez lê o arquivo .env
    private final String apiKey;
    private final String steamIdPadrao;

    @Autowired
    public SteamService(RestTemplate restTemplate, JogoRepository jogoRepository,
                        @Value("${steam.api-key}") String apiKey,
                        @Value("${steam.steam-id}") String steamIdPadrao) {
        this.restTemplate = restTemplate;
        this.jogoRepository = jogoRepository;
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
