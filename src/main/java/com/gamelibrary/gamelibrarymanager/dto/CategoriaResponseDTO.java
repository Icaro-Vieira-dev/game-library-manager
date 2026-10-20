package com.gamelibrary.gamelibrarymanager.dto;

import com.gamelibrary.gamelibrarymanager.model.Categoria;

import java.util.List;

// No JSON, os campos saem na ordem em que estão declarados aqui: idCategoria, nome, jogos
public record CategoriaResponseDTO(
        Long idCategoria,
        String nome,
        List<JogoResponseDTO> jogos
) {
    public static CategoriaResponseDTO deCategoria(Categoria categoria){
        return new CategoriaResponseDTO(
                categoria.getId(),
                categoria.getNome(),
                categoria.getJogos().stream().map(JogoResponseDTO::deJogo).toList());
    }
}
