package com.gamelibrary.gamelibrarymanager.dto;

import com.gamelibrary.gamelibrarymanager.model.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryRequestDTO {
    @NotBlank(message = "O nome da categoria não pode estar vazio.")
    @Size(max = 100, message = "O nome da categoria pode ter no máximo 100 caracteres.")
    private String nome;

    // Getter
    public String getNome() {
        return nome;
    }

    //Setter

    public void setNome(String nome) {
        this.nome = nome;
    }

    // strip() tira espaços do começo e do fim: "  RPG " vira "RPG"
    public Categoria toCategoria(){
        return new Categoria(this.nome.strip());
    }

}
