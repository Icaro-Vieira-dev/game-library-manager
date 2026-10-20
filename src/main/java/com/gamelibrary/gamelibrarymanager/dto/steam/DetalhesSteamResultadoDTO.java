package com.gamelibrary.gamelibrarymanager.dto.steam;

// Resumo devolvido pelo POST /steam/detalhes
public record DetalhesSteamResultadoDTO(
        int processados,      // quantos jogos foram consultados nesta chamada
        int atualizados,      // quantos receberam gênero/categorias/desenvolvedora/ano
        int semDadosNaLoja,   // quantos não estão mais na loja (removidos) e foram marcados para não buscar de novo
        long restantes,       // quantos ainda faltam buscar (chame de novo para continuar)
        String mensagem
) {
}
