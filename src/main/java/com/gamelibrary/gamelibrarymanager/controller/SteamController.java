package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.dto.JogoResponseDTO;
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

    /**
     * POST /importSteam/importar
     * Importa a biblioteca da Steam. Jogos que já foram importados antes são ignorados.
     *
     * Antes de usar: preencha STEAM_API_KEY e STEAM_ID no arquivo .env (raiz do projeto) e reinicie a aplicação.
     *
     * Exemplos:
     * POST http://localhost:8080/importSteam/importar                             -> usa o STEAM_ID do .env
     * POST http://localhost:8080/importSteam/importar?steamId=76561198000000000    -> importa outro perfil
     *
     * Resposta: 200 com a lista dos jogos NOVOS ([] se não havia nada novo).
     * Erro 422: chave não configurada ou recusada, steamId inválido, ou perfil privado (a mensagem diz qual).
     */
    @PostMapping("/importar")
    public List<JogoResponseDTO> importarSteamId(@RequestParam(required = false) String steamId) {
        List<Jogo> steam = steamService.importarJogos(steamId);
        List<JogoResponseDTO> jogosImportados = new ArrayList<>();

        for (Jogo jogo : steam) {
            jogosImportados.add(JogoResponseDTO.fromJogo(jogo));
        }

        return jogosImportados;

    }
}
