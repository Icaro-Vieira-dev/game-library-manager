package com.gamelibrary.gamelibrarymanager.dto.steam;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Resposta da loja da Steam (store.steampowered.com/api/featuredcategories): as seções da página inicial.
// Formato: { "0": {...}, "1": {...}, "specials": {...}, "coming_soon": { "items": [...] },
//            "top_sellers": {...}, "new_releases": { "items": [...] }, ... }
// Só mapeamos "new_releases" e "coming_soon"; o Jackson ignora as outras chaves.
public record SteamDestaquesResponseDTO(
        @JsonProperty("new_releases") Secao lancamentos,
        @JsonProperty("coming_soon") Secao emBreve
) {

    // uma seção da página: só a lista de itens nos interessa
    public record Secao(List<Item> items) {
        // construtor compacto: se "items" vier null ou faltar, vira lista vazia
        public Secao {
            items = items == null ? List.of() : items;
        }
    }

    // Um item de uma seção, ex.:
    // {"id":5423900,"type":0,"name":"Cashflow Empire","discounted":true,"discount_percent":40,
    //  "original_price":1449,"final_price":869,"currency":"BRL","header_image":"https://.../header.jpg", ...}
    // Números como Integer/Long (e não int/long): se vierem null, não quebram a leitura (Jackson 3).
    public record Item(
            // o appid do jogo na Steam
            Long id,
            // 0 = jogo, 1 = pacote/bundle
            Integer type,
            String name,
            // banner horizontal (460x215)
            @JsonProperty("header_image") String imagem,
            // preços em CENTAVOS: 869 = R$ 8,69 (a moeda depende do ?cc= da requisição).
            // No "Em breve" o preço vem 0, porque o jogo ainda não está à venda.
            @JsonProperty("final_price") Integer precoFinal,
            @JsonProperty("original_price") Integer precoOriginal,
            // desconto em %, ex.: 40 (0 = sem desconto)
            @JsonProperty("discount_percent") Integer desconto,
            // ex.: "BRL"
            String currency
    ) {
    }
}
