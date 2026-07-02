package com.gamelibrary.gamelibrarymanager.dto;

import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class CategoryRequestDTO {
    @NotBlank
    private String nome;

    // Getter
    public String getNome() {
        return nome;
    }

    //Setter

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Categoria toCategoria(){
        return new Categoria(this.nome);
    }

}
