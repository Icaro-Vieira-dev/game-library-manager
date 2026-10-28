package com.gamelibrary.gamelibrarymanager.dto;

import java.time.OffsetDateTime;

// USO INTERNO: é o que o UsuarioService.login devolve para o AuthController.
// NÃO vai mais para o JSON: o AuthController coloca o token no cookie httpOnly e responde só
// { expiraEm } (LoginResponseDTO).
public record TokenResponseDTO(
        String token,
        // sempre "Bearer": o tipo de token, como o cabeçalho Authorization espera
        String tipo,
        // quando o token vence (UTC). Depois disso, é preciso fazer login de novo.
        OffsetDateTime expiraEm
) {
    public static TokenResponseDTO bearer(String token, OffsetDateTime expiraEm) {
        return new TokenResponseDTO(token, "Bearer", expiraEm);
    }

    // O token é uma credencial: quem tem ele age como o usuário até ele vencer.
    // Se alguém logar este objeto por engano, o token não aparece.
    @Override
    public String toString() {
        return "TokenResponseDTO[token=***, tipo=" + tipo + ", expiraEm=" + expiraEm + "]";
    }
}
