package com.gamelibrary.gamelibrarymanager.service;

import com.gamelibrary.gamelibrarymanager.dto.steam.ImportacaoSteamResultadoDTO;
import com.gamelibrary.gamelibrarymanager.exception.SteamImportacaoException;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.repository.JogoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Testa a conversa HTTP do SteamService com a Steam (item 11, RestClient), SEM chamar a Steam de verdade:
// - MockRestServiceServer: um "servidor falso" ligado ao RestClient, que devolve as respostas que escrevemos;
// - mock() do Mockito: um repositório "de mentira", que não precisa de banco.
class SteamServiceHttpTest {

    private static final String STEAM_ID = "76561198000000000";

    private MockRestServiceServer steamFalsa;
    private JogoRepository jogoRepository;
    private SteamService steamService;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        steamFalsa = MockRestServiceServer.bindTo(builder).build();

        jogoRepository = mock(JogoRepository.class);
        // saveAll devolve a própria lista recebida (como se tivesse salvado)
        when(jogoRepository.saveAll(anyList())).thenAnswer(chamada -> chamada.getArgument(0));
        when(jogoRepository.findBySteamAppIdIsNotNull()).thenReturn(List.of());

        steamService = new SteamService(builder.build(), jogoRepository, mock(CategoriaService.class), "chave-de-teste", STEAM_ID);
    }

    @Test
    void importacao_leARespostaDaSteamECadastraOsJogos() {
        steamFalsa.expect(requestTo(org.hamcrest.Matchers.startsWith("https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/")))
                .andExpect(queryParam("steamid", STEAM_ID))
                .andExpect(queryParam("key", "chave-de-teste"))
                .andRespond(withSuccess("""
                        { "response": { "games": [
                            { "appid": 10, "name": "Hades", "playtime_forever": 120 },
                            { "appid": 20, "name": "Portal", "playtime_forever": 0 } ] } }
                        """, MediaType.APPLICATION_JSON));

        ImportacaoSteamResultadoDTO resultado = steamService.importarJogos(null);

        assertThat(resultado.jogosNovos()).isEqualTo(2);
        assertThat(resultado.novos()).extracting(j -> j.nomeJogo()).containsExactly("Hades", "Portal");
        steamFalsa.verify();   // confere que a chamada esperada aconteceu
    }

    @Test
    void chaveRecusadaPelaSteam_viraErroExplicado() {
        steamFalsa.expect(requestTo(org.hamcrest.Matchers.startsWith("https://api.steampowered.com/")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> steamService.importarJogos(null))
                .isInstanceOf(SteamImportacaoException.class)
                .hasMessageContaining("recusou a API key");
    }

    @Test
    void perfilPrivado_semListaDeJogos() {
        steamFalsa.expect(requestTo(org.hamcrest.Matchers.startsWith("https://api.steampowered.com/")))
                .andRespond(withSuccess("{ \"response\": {} }", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> steamService.importarJogos(null))
                .isInstanceOf(SteamImportacaoException.class)
                .hasMessageContaining("não retornou jogos");
    }

    @Test
    void jogoExistente_soAtualizaAsHoras() {
        Jogo hades = new Jogo();
        hades.setSteamAppId(10L);
        hades.setNomeJogo("Hades");
        hades.setHorasJogadas(1.0);
        when(jogoRepository.findBySteamAppIdIsNotNull()).thenReturn(List.of(hades));

        steamFalsa.expect(requestTo(org.hamcrest.Matchers.startsWith("https://api.steampowered.com/")))
                .andRespond(withSuccess("""
                        { "response": { "games": [ { "appid": 10, "name": "Hades", "playtime_forever": 300 } ] } }
                        """, MediaType.APPLICATION_JSON));

        ImportacaoSteamResultadoDTO resultado = steamService.importarJogos(null);

        assertThat(resultado.jogosNovos()).isZero();
        assertThat(resultado.horasAtualizadas()).isEqualTo(1);
        assertThat(hades.getHorasJogadas()).isEqualTo(5.0);
    }
}
