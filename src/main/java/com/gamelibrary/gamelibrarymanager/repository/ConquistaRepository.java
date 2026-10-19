package com.gamelibrary.gamelibrarymanager.repository;

import com.gamelibrary.gamelibrarymanager.model.Conquista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ConquistaRepository extends JpaRepository<Conquista, Long> {
    // "Jogo_IdJogo" = navega de Conquista.jogo até Jogo.idJogo
    List<Conquista> findByJogo_IdJogoOrderByIdAsc(Long idJogo);

    List<Conquista> findByJogo_IdJogoAndConcluidaOrderByIdAsc(Long idJogo, boolean concluida);

    // um único DELETE, em vez de carregar e apagar uma por uma
    @Modifying
    @Query("delete from Conquista c where c.jogo.idJogo = :idJogo")
    void deleteByJogoId(Long idJogo);
}
