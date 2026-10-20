package com.gamelibrary.gamelibrarymanager.dto.steam;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Resposta da loja da Steam (store.steampowered.com/api/appdetails) para UM jogo.
// O JSON vem dentro de uma chave com o próprio appid: { "1245620": { "success": true, "data": { ... } } }
// Jogo que não existe mais na loja: { "999": { "success": false } } (sem "data")
// success é Boolean (e não boolean) pelo mesmo motivo do SteamJogoDTO: se faltar, vira false, em vez de quebrar
public record SteamLojaAppDTO(Boolean success, Dados data) {
    public SteamLojaAppDTO {
        success = Boolean.TRUE.equals(success);
    }

    // só os campos que usamos; o Jackson ignora o resto do JSON
    public record Dados(
            @JsonProperty("genres") List<Genero> generos,
            @JsonProperty("developers") List<String> desenvolvedoras,
            @JsonProperty("release_date") DataLancamento dataLancamento,
            // URL do banner horizontal; nos jogos novos o caminho tem um código no meio, por isso vale guardar a URL pronta
            @JsonProperty("header_image") String imagemBanner
    ) {
    }

    // ex.: { "id": "3", "description": "RPG" }
    public record Genero(@JsonProperty("description") String descricao) {
    }

    // ex.: { "coming_soon": false, "date": "24/fev./2022" } (o formato da data muda com o idioma)
    public record DataLancamento(@JsonProperty("date") String data) {
    }
}
