package com.gamelibrary.gamelibrarymanager.repository;

import com.gamelibrary.gamelibrarymanager.model.Conquista;
import com.gamelibrary.gamelibrarymanager.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// A conquista não tem dono próprio: o dono é o do JOGO. Toda consulta filtra pelo dono do jogo
// dentro da própria consulta ("Jogo_Usuario" = navega de Conquista.jogo até Jogo.usuario).
public interface ConquistaRepository extends JpaRepository<Conquista, Long> {
    // "Jogo_IdJogo" = navega de Conquista.jogo até Jogo.idJogo
    List<Conquista> findByJogo_IdJogoAndJogo_UsuarioOrderByIdAsc(Long idJogo, Usuario usuario);

    List<Conquista> findByJogo_IdJogoAndJogo_UsuarioAndConcluidaOrderByIdAsc(Long idJogo, Usuario usuario, boolean concluida);

    // um único DELETE, em vez de carregar e apagar uma por uma. O dono vai num subselect porque o
    // DELETE em lote do JPA não aceita "c.jogo.usuario" direto no WHERE.
    @Modifying
    @Query("delete from Conquista c where c.jogo.idJogo in "
            + "(select j.idJogo from Jogo j where j.idJogo = :idJogo and j.usuario = :usuario)")
    void apagarDoJogo(Long idJogo, Usuario usuario);
}
