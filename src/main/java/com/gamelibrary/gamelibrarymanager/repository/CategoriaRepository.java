package com.gamelibrary.gamelibrarymanager.repository;

import com.gamelibrary.gamelibrarymanager.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByNomeContainingIgnoreCase(String nome);

    // nome exato, sem diferenciar maiúsculas ("rpg" acha "RPG"); First = se houver repetidas, pega a primeira
    Optional<Categoria> findFirstByNomeIgnoreCase(String nome);

}
