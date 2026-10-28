package com.gamelibrary.gamelibrarymanager.dto;

import java.math.BigDecimal;

// Um jogo da vitrine (GET /vitrine), como a NOSSA API devolve para o front-end.
// Nenhum nome em inglês da Steam aparece aqui: a conversão fica no VitrineService.
public record JogoVitrineDTO(
        // o appid do jogo na Steam
        Long appId,
        String nome,
        // banner horizontal (460x215)
        String imagem,
        // preços em REAIS, ex.: 8.69. BigDecimal (e não double) porque dinheiro não pode ter
        // erro de arredondamento. null quando a Steam manda 0 (no "Em breve" o jogo ainda não está à venda)
        BigDecimal preco,
        BigDecimal precoOriginal,
        // desconto em %, ex.: 40 (0 = sem desconto)
        Integer desconto,
        // o texto que a Steam devolve, em português, ex.: "28/out./2026" ou "Em breve". Pode ser null.
        String dataLancamento,
        // página do jogo na loja: https://store.steampowered.com/app/{appId}
        String urlLoja
) {
}
