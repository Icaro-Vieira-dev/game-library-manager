package com.gamelibrary.gamelibrarymanager.repository;

import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// REGRA: toda consulta daqui filtra pelo DONO dentro da própria consulta. As buscas por nome usam o
// nomeNormalizado (Categoria.normalizarNome): passe o texto já normalizado.
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByUsuario(Usuario usuario);

    // a categoria, se ela existir E for desse usuário (de outro usuário = vazio = 404)
    Optional<Categoria> findByIdAndUsuario(Long id, Usuario usuario);

    // nome CONTÉM o texto ("rp" acha "RPG")
    List<Categoria> findByUsuarioAndNomeNormalizadoContaining(Usuario usuario, String nomeNormalizado);

    // nome exato ("rpg" acha "RPG"). É único por usuário, então no máximo uma.
    Optional<Categoria> findByUsuarioAndNomeNormalizado(Usuario usuario, String nomeNormalizado);
}
