package com.gamelibrary.gamelibrarymanager.repository;

import com.gamelibrary.gamelibrarymanager.dto.FiltroJogosDTO;
import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

// Monta a consulta da busca GET /jogos a partir dos filtros que vierem preenchidos.
// Uma Specification é um "pedaço de WHERE" escrito em Java: em vez de um método no repositório
// para cada combinação de filtros, montamos só as condições pedidas e juntamos com AND.
public final class JogoEspecificacoes {

    private JogoEspecificacoes() {
        // classe só de métodos estáticos: não faz sentido criar objetos dela
    }

    public static Specification<Jogo> comFiltros(FiltroJogosDTO filtro) {
        return (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();

            if (temTexto(filtro.nome())) {
                // LIKE '%ring%' sem diferenciar maiúsculas: lower(nome) like '%ring%'
                condicoes.add(cb.like(cb.lower(root.get("nomeJogo")), "%" + filtro.nome().strip().toLowerCase() + "%"));
            }
            if (temTexto(filtro.genero())) {
                condicoes.add(cb.equal(cb.lower(root.get("generoJogo")), filtro.genero().strip().toLowerCase()));
            }
            if (temTexto(filtro.plataforma())) {
                condicoes.add(cb.equal(cb.lower(root.get("plataformaJogo")), filtro.plataforma().strip().toLowerCase()));
            }
            if (filtro.status() != null) {
                condicoes.add(cb.equal(root.get("statusJogo"), filtro.status()));
            }
            if (filtro.anoLancamento() != null) {
                condicoes.add(cb.equal(root.get("anoLancamentoJogo"), filtro.anoLancamento()));
            }
            if (temTexto(filtro.categoria())) {
                // "entra" na lista de categorias do jogo (o JOIN com a tabela jogo_categorias)
                Join<Jogo, Categoria> categorias = root.join("categorias");
                condicoes.add(cb.equal(cb.lower(categorias.get("nome")), filtro.categoria().strip().toLowerCase()));
                // sem o distinct, um jogo apareceria uma vez para cada categoria que bate
                query.distinct(true);
            }

            // nenhum filtro = nenhuma condição = todos os jogos
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
