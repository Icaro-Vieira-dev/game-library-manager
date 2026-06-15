package com.gamelibrary.gamelibrarymanager.repository;

import com.gamelibrary.gamelibrarymanager.model.Jogo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JogoRepository extends JpaRepository<Jogo, Long> {
}
