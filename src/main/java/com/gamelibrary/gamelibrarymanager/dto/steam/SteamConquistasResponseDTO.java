package com.gamelibrary.gamelibrarymanager.dto.steam;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Formato da resposta do GetPlayerAchievements:
// { "playerstats": { "success": true, "achievements": [ ... ] } }
// ou, em caso de erro: { "playerstats": { "success": false, "error": "Requested app has no stats" } }
public record SteamConquistasResponseDTO(PlayerStats playerstats) {

    public record PlayerStats(
            Boolean success,
            String error,
            @JsonProperty("achievements") List<SteamConquistaDTO> conquistas
    ) {
        // Boolean + construtor compacto: se "success" faltar, vira false (mesmo motivo do SteamJogoDTO)
        public PlayerStats {
            success = Boolean.TRUE.equals(success);
        }
    }
}
