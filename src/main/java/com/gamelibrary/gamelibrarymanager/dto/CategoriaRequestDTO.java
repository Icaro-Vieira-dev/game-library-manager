package com.gamelibrary.gamelibrarymanager.dto;

import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.model.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Body do POST /categorias: { "nome": "RPG" }
public record CategoriaRequestDTO(
        @NotBlank(message = "O nome da categoria não pode estar vazio.")
        @Size(max = 100, message = "O nome da categoria pode ter no máximo 100 caracteres.")
        String nome
) {
    // strip() tira espaços do começo e do fim: "  RPG " vira "RPG".
    // dono = o usuário logado (vem do @UsuarioLogado do controller, nunca do body)
    public Categoria toCategoria(Usuario dono){
        return new Categoria(this.nome.strip(), dono);
    }
}
