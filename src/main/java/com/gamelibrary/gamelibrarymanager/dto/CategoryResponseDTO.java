package com.gamelibrary.gamelibrarymanager.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.model.Jogo;

import java.util.List;
import java.util.stream.Collectors;

@JsonPropertyOrder({"id","nome", "jogos"})
public class CategoryResponseDTO {
    private Long idCategoria;
    private String nome;
    private List<JogoResponseDTO> jogos;

    // Getters


    public Long getIdCategoria() {
        return idCategoria;
    }

    public String getNome() {
        return nome;
    }

    public List<JogoResponseDTO> getJogos() {
        return jogos;
    }

    // setters

    public void setIdCategoria(Long idCategoria) {
        this.idCategoria = idCategoria;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setJogos(List<JogoResponseDTO> jogos) {
        this.jogos = jogos;
    }

    public static CategoryResponseDTO fromCategory(Categoria categoria){
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setIdCategoria(categoria.getId());
        dto.setNome(categoria.getNome());
        dto.setJogos(categoria.getJogos().stream().map(JogoResponseDTO::fromJogo).collect(Collectors.toList()));
        return dto;
    }
}
