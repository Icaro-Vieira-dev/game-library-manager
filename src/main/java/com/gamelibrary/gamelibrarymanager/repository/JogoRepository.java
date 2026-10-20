package com.gamelibrary.gamelibrarymanager.repository;

import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // quantos jogos estão nessa categoria ("Categorias_Id" = entra em categorias e compara o id)
    long countByCategorias_Id(Long categoriaId);

    // apaga todas as linhas da tabela de vínculo (SQL nativo, porque jogo_categorias não é uma entidade)
    @Modifying
    @Query(value = "delete from jogo_categorias", nativeQuery = true)
    void apagarTodosVinculosComCategorias();

    // total de vínculos jogo-categoria (cada linha da tabela jogo_categorias)
    @Query("select count(j) from Jogo j join j.categorias")
    long contarVinculosComCategorias();

    // jogos da Steam que ainda não tiveram os detalhes da loja buscados; Limit = quantos trazer de uma vez
    List<Jogo> findBySteamAppIdIsNotNullAndDetalhesLojaBuscadosIsNullOrderByIdJogoAsc(Limit limit);

    long countBySteamAppIdIsNotNullAndDetalhesLojaBuscadosIsNull();
}
