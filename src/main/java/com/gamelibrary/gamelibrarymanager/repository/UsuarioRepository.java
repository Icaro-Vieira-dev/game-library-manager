package com.gamelibrary.gamelibrarymanager.repository;

import com.gamelibrary.gamelibrarymanager.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// O e-mail recebido aqui precisa já estar normalizado (Usuario.normalizarEmail):
// a busca é exata, e no banco ele está guardado em minúsculas.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsBySteamId(String steamId);
}
