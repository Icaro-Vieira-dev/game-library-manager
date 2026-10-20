package com.gamelibrary.gamelibrarymanager.dto.steam;

import com.fasterxml.jackson.annotation.JsonProperty;

// Uma conquista como a Steam devolve no GetPlayerAchievements.
// O @JsonProperty liga o nome do JSON da Steam (à direita) ao nome em Java (à esquerda).
// Números como Integer/Long + construtor compacto: mesmo motivo explicado no SteamJogoDTO.
public record SteamConquistaDTO(
        @JsonProperty("apiname") String apiName,
        // 1 = desbloqueada, 0 = não
        @JsonProperty("achieved") Integer desbloqueada,
        // quando desbloqueou, em segundos desde 1970; 0 se não desbloqueou
        @JsonProperty("unlocktime") Long momentoDesbloqueio,
        @JsonProperty("name") String nome,
        @JsonProperty("description") String descricao
) {
    public SteamConquistaDTO {
        desbloqueada = desbloqueada == null ? 0 : desbloqueada;
        momentoDesbloqueio = momentoDesbloqueio == null ? 0L : momentoDesbloqueio;
    }
}
