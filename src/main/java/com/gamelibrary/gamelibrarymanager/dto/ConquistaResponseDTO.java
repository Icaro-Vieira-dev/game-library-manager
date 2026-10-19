package com.gamelibrary.gamelibrarymanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gamelibrary.gamelibrarymanager.model.Conquista;

import java.time.Instant;

public class ConquistaResponseDTO {
    private String nome;
    private String descricao;
    private boolean concluida;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Instant dataDesbloqueio;

    // Getter
    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isConcluida() {
        return concluida;
    }

    public Instant getDataDesbloqueio() {
        return dataDesbloqueio;
    }

    // Setter
    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public void setConcluida(boolean concluida) {
        this.concluida = concluida;
    }

    public void setDataDesbloqueio(Instant dataDesbloqueio) {
        this.dataDesbloqueio = dataDesbloqueio;
    }

    public static ConquistaResponseDTO fromConquista(Conquista conquista) {
        ConquistaResponseDTO dto = new ConquistaResponseDTO();
        dto.setNome(conquista.getNome());
        dto.setDescricao(conquista.getDescricao());
        dto.setConcluida(conquista.isConcluida());
        dto.setDataDesbloqueio(conquista.getDataDesbloqueio());
        return dto;
    }
}
