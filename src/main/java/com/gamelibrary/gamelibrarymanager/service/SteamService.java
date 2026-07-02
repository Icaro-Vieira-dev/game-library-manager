package com.gamelibrary.gamelibrarymanager.service;

import com.gamelibrary.gamelibrarymanager.dto.steam.SteamGameDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamResponseDTO;
import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.repository.JogoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class SteamService {
    private final RestTemplate restTemplate;
    private final JogoRepository jogoRepository;

    @Autowired
    public SteamService(RestTemplate restTemplate, JogoRepository jogoRepository) {
        this.restTemplate = restTemplate;
        this.jogoRepository = jogoRepository;
    }


    public List<Jogo> importarJogos(String steamId, String apiKey){
        String url = String.format(
                "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=%s&steamid=%s&include_appinfo=true",
                apiKey,
                steamId
        );

        // get na url
        SteamResponseDTO responseDTO = restTemplate.getForObject(url, SteamResponseDTO.class);
        List<Jogo> jogoSalvo = new ArrayList<>();

        for (SteamGameDTO steamGameDTO: responseDTO.getResponse().getGames()){
            Jogo jogo = new Jogo();
            jogo.setNomeJogo(steamGameDTO.getName());
            jogo.setHorasJogadas(steamGameDTO.getPlaytime_forever() / 60.0);
            jogo.setStatusJogo(GameStatus.PLANEJADO);
            jogoSalvo.add(jogoRepository.save(jogo));

        }
        return jogoSalvo;
    }

}
