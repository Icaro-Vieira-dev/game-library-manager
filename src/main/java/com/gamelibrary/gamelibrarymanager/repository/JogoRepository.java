package com.gamelibrary.gamelibrarymanager.repository;

import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JogoRepository extends JpaRepository<Jogo, Long> {
    List<Jogo>findByNomeJogoContainingIgnoreCase(String nome);
    List<Jogo> findByGeneroJogo(String genero);
    List<Jogo> findByPlataformaJogo(String plataforma);
    List<Jogo> findByStatusJogo(GameStatus status);
    List<Jogo> findByAnoLancamentoJogo(int ano);
}
