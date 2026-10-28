package com.gamelibrary.gamelibrarymanager.dto;

import com.gamelibrary.gamelibrarymanager.model.Usuario;

import java.time.OffsetDateTime;

// Um usuário como a API devolve. NUNCA tem senha nem hash: o senhaHash da entidade fica de fora.
// Por enquanto só é devolvido ao PRÓPRIO dono (cadastro e GET /usuarios/me), por isso pode ter
// e-mail e Steam ID.
public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        // null se o usuário não informou
        String steamId,
        OffsetDateTime criadoEm
) {
    public static UsuarioResponseDTO deUsuario(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getSteamId(),
                usuario.getCriadoEm());
    }
}
