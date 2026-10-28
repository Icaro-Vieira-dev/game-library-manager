package com.gamelibrary.gamelibrarymanager.dto;

import java.time.OffsetDateTime;

// Corpo da resposta do POST /auth/login. O token NÃO vem aqui: ele vai no cookie httpOnly
// (cabeçalho Set-Cookie), que o JavaScript do front não consegue ler.
public record LoginResponseDTO(
        // quando o login vence (UTC). Depois disso, é preciso entrar de novo.
        OffsetDateTime expiraEm
) {
}
