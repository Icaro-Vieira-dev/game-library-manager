package com.gamelibrary.gamelibrarymanager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="Categoria")
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String nome;

    @OneToMany(mappedBy = "categoria")
    private List<Jogo> jogos = new ArrayList<>();

    // construtor
    public Categoria() {}
    public Categoria(String nome) {
        this.nome = nome;
    }

    // Getter
    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public List<Jogo> getJogos() {
        return jogos;
    }


    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
