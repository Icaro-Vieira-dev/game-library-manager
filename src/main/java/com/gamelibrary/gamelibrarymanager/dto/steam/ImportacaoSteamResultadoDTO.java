package com.gamelibrary.gamelibrarymanager.dto.steam;

import com.gamelibrary.gamelibrarymanager.dto.JogoResponseDTO;

import java.util.List;

// Resposta do POST de importação da Steam: o que entrou de novo e quantos jogos tiveram as horas atualizadas.
public record ImportacaoSteamResultadoDTO(
        int jogosNovos,                 // quantos jogos foram cadastrados agora
        int horasAtualizadas,           // quantos jogos que já existiam tiveram as horas jogadas atualizadas
        List<JogoResponseDTO> novos     // os jogos novos ([] se não havia nada novo)
) {
}
