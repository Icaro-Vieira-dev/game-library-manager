package com.gamelibrary.gamelibrarymanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Body do POST /auth/login: { "email": "icaro@email.com", "senha": "uma-senha-forte" }
// Sem @Email e sem tamanho mínimo de senha: no login só importa se confere ou não.
// Os tamanhos máximos evitam mandar um texto gigante de propósito para o Argon2 (lento de propósito)
// e travar o servidor.
public record LoginRequestDTO(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Size(max = 254, message = "O e-mail pode ter no máximo 254 caracteres.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(max = 128, message = "A senha pode ter no máximo 128 caracteres.")
        String senha
) {
    // O toString() padrão de um record mostra todos os campos, inclusive a senha.
    // Se alguém logar este objeto por engano, a senha não aparece.
    @Override
    public String toString() {
        return "LoginRequestDTO[email=" + email + ", senha=***]";
    }
}
