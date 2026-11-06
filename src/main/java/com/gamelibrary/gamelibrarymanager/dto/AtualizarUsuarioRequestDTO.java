package com.gamelibrary.gamelibrarymanager.dto;

import jakarta.validation.constraints.Pattern;

// Body do PATCH /usuarios/me: { "steamId": "76561198000000000" }
// Por enquanto só o Steam ID pode ser trocado (nome, e-mail e senha ficam para outra etapa).
// O id do usuário NÃO vem aqui: vem do token.
public record AtualizarUsuarioRequestDTO(
        // mesma regra do cadastro (CadastroRequestDTO): SteamID64, 17 dígitos começando com 7656119.
        // O @Pattern só confere quando o campo vem preenchido: null passa (e significa "remover").
        @Pattern(regexp = "^7656119\\d{10}$",
                message = "Steam ID inválido: use o SteamID64, com 17 dígitos começando com 7656119 "
                        + "(aparece em steamcommunity.com/profiles/7656... ou em https://steamid.io).")
        String steamId
) {
    // Construtor compacto: roda quando o JSON é lido, ANTES da validação. Tira os espaços das pontas;
    // vazio ou só espaços vira null (= remover o Steam ID).
    // Atenção: num record não dá para diferenciar "campo ausente" de null, então um body {} também REMOVE.
    public AtualizarUsuarioRequestDTO {
        steamId = (steamId == null || steamId.isBlank()) ? null : steamId.strip();
    }
}
