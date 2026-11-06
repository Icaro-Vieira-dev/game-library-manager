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
        Long dono = categoria.getUsuario().getId();
        return new CategoriaResponseDTO(
                categoria.getId(),
                categoria.getNome(),
                categoria.getJogos().stream()
                        // DEFESA EXTRA: hoje um vínculo jogo-categoria só nasce entre coisas do MESMO dono
                        // (JogoService.vincularCategoria e SteamService.aplicarDadosLoja exigem isso), então
                        // este filtro não tira nada. Ele existe para o caso de essa regra ser quebrada no futuro:
                        // mesmo assim, ninguém vê o jogo de outra pessoa pela categoria.
                        // Custo zero: o getUsuario() é LAZY e o getId() de um proxy LAZY não consulta o banco.
                        .filter(jogo -> dono.equals(jogo.getUsuario().getId()))
                        .map(JogoResponseDTO::deJogo)
                        .toList());
    }
}
