package com.gamelibrary.gamelibrarymanager.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// Resposta paginada: os itens da página + as informações para navegar entre as páginas.
// <T> = o tipo dos itens (ex.: JogoResponseDTO). Um record "genérico" serve para qualquer lista.
public record PaginaResponseDTO<T>(
        List<T> conteudo,      // os itens desta página
        int pagina,            // número da página atual (começa em 0)
        int tamanho,           // quantos itens por página foram pedidos
        long totalElementos,   // quantos itens existem no total, somando todas as páginas
        int totalPaginas,      // quantas páginas existem
        boolean ultima         // true se esta é a última página
) {
    // converte a Page do Spring Data, transformando cada item com a função "conversor"
    // (ex.: JogoResponseDTO::deJogo transforma Jogo em JogoResponseDTO)
    public static <E, T> PaginaResponseDTO<T> de(Page<E> page, Function<E, T> conversor) {
        return new PaginaResponseDTO<>(
                page.getContent().stream().map(conversor).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
