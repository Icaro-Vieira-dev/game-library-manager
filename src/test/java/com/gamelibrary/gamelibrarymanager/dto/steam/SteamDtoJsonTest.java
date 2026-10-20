package com.gamelibrary.gamelibrarymanager.dto.steam;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

// Garante que os records da Steam leem o JSON no formato que a Steam manda de verdade.
// Os nomes do JSON ("playtime_forever") são diferentes dos nomes em Java ("minutosJogados"):
// quem liga um ao outro é o @JsonProperty. Se alguém apagar ou errar um, este teste quebra.
class SteamDtoJsonTest {

    // como o Spring faz: campos do JSON que o record não tem são ignorados
    private final JsonMapper json = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    void jogoDaBiblioteca() {
        String daSteam = """
                { "response": { "game_count": 1, "games": [
                    { "appid": 1245620, "name": "ELDEN RING", "playtime_forever": 5022,
                      "playtime_2weeks": 90, "rtime_last_played": 1736050418, "img_icon_url": "abc" } ] } }
                """;

        SteamJogoDTO jogo = json.readValue(daSteam, SteamBibliotecaResponseDTO.class).response().jogos().get(0);

        assertThat(jogo.appid()).isEqualTo(1245620);
        assertThat(jogo.name()).isEqualTo("ELDEN RING");
        assertThat(jogo.minutosJogados()).isEqualTo(5022);
        assertThat(jogo.minutosUltimas2Semanas()).isEqualTo(90);
        assertThat(jogo.ultimaVezJogado()).isEqualTo(1736050418L);
    }

    @Test
    void campoQueASteamNaoMandaViraZero() {
        // a Steam só manda o playtime_2weeks se for maior que 0
        String daSteam = """
                { "appid": 10, "name": "Portal", "playtime_forever": 120 }
                """;

        SteamJogoDTO jogo = json.readValue(daSteam, SteamJogoDTO.class);

        assertThat(jogo.minutosUltimas2Semanas()).isZero();
        assertThat(jogo.ultimaVezJogado()).isZero();
    }

    @Test
    void conquista() {
        String daSteam = """
                { "playerstats": { "success": true, "achievements": [
                    { "apiname": "ACH_01", "achieved": 1, "unlocktime": 1736050418,
                      "name": "Portador de Fragmento", "description": "" } ] } }
                """;

        SteamConquistasResponseDTO resposta = json.readValue(daSteam, SteamConquistasResponseDTO.class);
        SteamConquistaDTO conquista = resposta.playerstats().conquistas().get(0);

        assertThat(resposta.playerstats().success()).isTrue();
        assertThat(conquista.apiName()).isEqualTo("ACH_01");
        assertThat(conquista.desbloqueada()).isEqualTo(1);
        assertThat(conquista.momentoDesbloqueio()).isEqualTo(1736050418L);
        assertThat(conquista.nome()).isEqualTo("Portador de Fragmento");
    }

    @Test
    void detalhesDaLoja() {
        String daSteam = """
                { "success": true, "data": {
                    "genres": [ { "id": "1", "description": "Ação" }, { "id": "3", "description": "RPG" } ],
                    "developers": [ "FromSoftware, Inc." ],
                    "release_date": { "coming_soon": false, "date": "24/fev./2022" },
                    "header_image": "https://exemplo/header.jpg" } }
                """;

        SteamLojaAppDTO app = json.readValue(daSteam, SteamLojaAppDTO.class);

        assertThat(app.success()).isTrue();
        assertThat(app.data().generos()).extracting(SteamLojaAppDTO.Genero::descricao).containsExactly("Ação", "RPG");
        assertThat(app.data().desenvolvedoras()).containsExactly("FromSoftware, Inc.");
        assertThat(app.data().dataLancamento().data()).isEqualTo("24/fev./2022");
        assertThat(app.data().imagemBanner()).isEqualTo("https://exemplo/header.jpg");
    }
}
