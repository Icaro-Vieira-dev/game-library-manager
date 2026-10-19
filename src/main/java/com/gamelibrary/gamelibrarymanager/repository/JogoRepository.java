package com.gamelibrary.gamelibrarymanager.repository;

import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface JogoRepository extends JpaRepository<Jogo, Long> {
    List<Jogo>findByNomeJogoContainingIgnoreCase(String nome);
    List<Jogo> findByGeneroJogo(String genero);
    List<Jogo> findByPlataformaJogo(String plataforma);
    List<Jogo> findByStatusJogo(GameStatus status);
    List<Jogo> findByAnoLancamentoJogo(int ano);

    @Query("select j.steamAppId from Jogo j where j.steamAppId is not null")
    Set<Long> findAllSteamAppIds();
}
