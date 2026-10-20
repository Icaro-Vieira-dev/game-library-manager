package com.gamelibrary.gamelibrarymanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gamelibrary.gamelibrarymanager.model.Conquista;

import java.time.Instant;

public record ConquistaResponseDTO(
        String nome,
        String descricao,
        boolean concluida,
        // só aparece no JSON se a conquista já foi desbloqueada
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant dataDesbloqueio
) {
    public static ConquistaResponseDTO deConquista(Conquista conquista) {
        return new ConquistaResponseDTO(
                conquista.getNome(),
                conquista.getDescricao(),
                conquista.isConcluida(),
                conquista.getDataDesbloqueio());
    }
}
