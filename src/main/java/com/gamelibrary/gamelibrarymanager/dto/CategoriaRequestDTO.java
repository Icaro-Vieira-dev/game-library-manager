package com.gamelibrary.gamelibrarymanager.dto;

import com.gamelibrary.gamelibrarymanager.model.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Body do POST /categorias: { "nome": "RPG" }
public record CategoriaRequestDTO(
        @NotBlank(message = "O nome da categoria não pode estar vazio.")
        @Size(max = 100, message = "O nome da categoria pode ter no máximo 100 caracteres.")
        String nome
) {
    // strip() tira espaços do começo e do fim: "  RPG " vira "RPG"
    public Categoria toCategoria(){
        return new Categoria(this.nome.strip());
    }
}
