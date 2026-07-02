package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.dto.JogoResponseDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamGameDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamResponseDTO;
import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.service.SteamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/importSteam")
public class SteamController {
    private final SteamService steamService;

    @Autowired
    public SteamController(SteamService steamService) {
        this.steamService = steamService;
    }

    @PostMapping("/importar")
    public List<JogoResponseDTO> importarSteamId(@RequestParam String steamId, @RequestParam String apiKey) {
        List<Jogo> steam = steamService.importarJogos(steamId, apiKey);
        List<JogoResponseDTO> jogosImportados = new ArrayList<>();

        for (Jogo jogo : steam) {
            jogosImportados.add(JogoResponseDTO.fromJogo(jogo));
        }

        return jogosImportados;

    }
}
