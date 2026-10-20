package com.gamelibrary.gamelibrarymanager.repository;

import com.gamelibrary.gamelibrarymanager.model.Jogo;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// JpaSpecificationExecutor = ganha o findAll(Specification, Pageable), usado na busca com filtros
// (veja JogoEspecificacoes). Ele substitui os antigos findByGeneroJogo, findByPlataformaJogo etc.
public interface JogoRepository extends JpaRepository<Jogo, Long>, JpaSpecificationExecutor<Jogo> {

    // todos os jogos que vieram da Steam (usado na importação para achar os que já existem)
    List<Jogo> findBySteamAppIdIsNotNull();

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
