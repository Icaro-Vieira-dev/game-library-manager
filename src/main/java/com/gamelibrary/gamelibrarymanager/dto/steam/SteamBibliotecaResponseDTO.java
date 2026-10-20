package com.gamelibrary.gamelibrarymanager.dto.steam;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Formato da resposta do GetOwnedGames: { "response": { "games": [ ... ] } }
public record SteamBibliotecaResponseDTO(SteamResponse response) {

    public record SteamResponse(@JsonProperty("games") List<SteamJogoDTO> jogos) {
    }
}
