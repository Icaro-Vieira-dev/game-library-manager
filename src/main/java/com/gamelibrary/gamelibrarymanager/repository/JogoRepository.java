package com.gamelibrary.gamelibrarymanager.repository;

import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.model.Usuario;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

// JpaSpecificationExecutor = ganha o findAll(Specification, Pageable), usado na busca com filtros
// (veja JogoEspecificacoes). Ele substitui os antigos findByGeneroJogo, findByPlataformaJogo etc.
//
// REGRA: toda consulta daqui filtra pelo DONO dentro da própria consulta (o "Usuario" no nome do método
// vira "and j.usuario = ?" no SQL). Nunca busque um jogo só pelo id e confira o dono depois.
// Os métodos herdados sem dono (findById, findAll, deleteById...) NÃO devem ser usados para jogos.
public interface JogoRepository extends JpaRepository<Jogo, Long>, JpaSpecificationExecutor<Jogo> {

    // o jogo, se ele existir E for desse usuário. De outro usuário = vazio (vira o mesmo 404 de "não existe")
    Optional<Jogo> findByIdJogoAndUsuario(Long idJogo, Usuario usuario);

    boolean existsByIdJogoAndUsuario(Long idJogo, Usuario usuario);

    // os jogos da Steam DESSE usuário (usado na importação para achar os que já existem)
    List<Jogo> findByUsuarioAndSteamAppIdIsNotNull(Usuario usuario);

    // quantos jogos DESSE usuário estão nessa categoria ("Categorias_Id" = entra em categorias e compara o id)
    long countByUsuarioAndCategorias_Id(Usuario usuario, Long categoriaId);

    // apaga os vínculos com categorias dos jogos DESSE usuário (SQL nativo, porque jogo_categorias não é
    // uma entidade). Precisa vir antes do apagarTodosDoUsuario, que não passa pelos vínculos.
    @Modifying
    @Query(value = "delete from jogo_categorias where jogo_id in "
            + "(select j.id_jogo from jogos j where j.usuario_id = :usuarioId)", nativeQuery = true)
    void apagarVinculosComCategoriasDoUsuario(Long usuarioId);

    // um único DELETE com todos os jogos DESSE usuário (as conquistas o banco apaga sozinho: ON DELETE CASCADE)
    @Modifying
    @Query("delete from Jogo j where j.usuario = :usuario")
    void apagarTodosDoUsuario(Usuario usuario);

    // total de vínculos jogo-categoria DESSE usuário (cada linha da tabela jogo_categorias dos jogos dele)
    @Query("select count(j) from Jogo j join j.categorias where j.usuario = :usuario")
    long contarVinculosComCategorias(Usuario usuario);

    // jogos da Steam DESSE usuário que ainda não tiveram os detalhes da loja buscados;
    // Limit = quantos trazer de uma vez
    List<Jogo> findByUsuarioAndSteamAppIdIsNotNullAndDetalhesLojaBuscadosIsNullOrderByIdJogoAsc(Usuario usuario, Limit limit);

    long countByUsuarioAndSteamAppIdIsNotNullAndDetalhesLojaBuscadosIsNull(Usuario usuario);
}
