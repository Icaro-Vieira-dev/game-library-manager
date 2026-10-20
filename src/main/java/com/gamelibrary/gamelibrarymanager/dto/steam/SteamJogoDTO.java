package com.gamelibrary.gamelibrarymanager.dto.steam;

import com.fasterxml.jackson.annotation.JsonProperty;

// Um jogo da biblioteca, como a Steam devolve no GetOwnedGames.
// O @JsonProperty liga o nome do JSON da Steam (ex.: "playtime_forever") a um nome claro em Java.
//
// Os números são Integer/Long (e não int/long) de propósito: a Steam NÃO manda alguns campos
// (ex.: playtime_2weeks de quem não jogou nas últimas 2 semanas). No Jackson 3, um campo primitivo
// ausente faz a leitura FALHAR; com o tipo objeto, ele chega como null e o construtor abaixo troca por 0.
public record SteamJogoDTO(
        Integer appid,
        String name,
        // total de minutos jogados
        @JsonProperty("playtime_forever") Integer minutosJogados,
        // minutos jogados nas últimas 2 semanas; a Steam só envia se for maior que 0
        @JsonProperty("playtime_2weeks") Integer minutosUltimas2Semanas,
        // última vez que jogou, em segundos desde 01/01/1970 (timestamp Unix); 0 se nunca jogou
        @JsonProperty("rtime_last_played") Long ultimaVezJogado
) {
    // construtor "compacto" de um record: roda antes de os valores serem guardados,
    // e aqui troca os null por 0. Assim quem usa nunca recebe null.
    public SteamJogoDTO {
        appid = appid == null ? 0 : appid;
        minutosJogados = minutosJogados == null ? 0 : minutosJogados;
        minutosUltimas2Semanas = minutosUltimas2Semanas == null ? 0 : minutosUltimas2Semanas;
        ultimaVezJogado = ultimaVezJogado == null ? 0L : ultimaVezJogado;
    }
}
