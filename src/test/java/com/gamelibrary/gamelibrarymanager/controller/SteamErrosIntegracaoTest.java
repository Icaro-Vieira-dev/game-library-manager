package com.gamelibrary.gamelibrarymanager.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Teste de INTEGRAÇÃO dos erros da Steam no POST /steam/importacao: a resposta certa para cada erro e,
// principalmente, que NADA de perigoso vai para o log nem para a resposta:
//   - a chave da API (vai na URL: ?key=...);
//   - o corpo cru da resposta da Steam (não sabemos o que vem nele).
// A Steam é FALSA (MockRestServiceServer): nenhum teste chama a Steam de verdade.
// OutputCaptureExtension: guarda tudo o que foi escrito no console (inclusive o log) durante o teste.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class SteamErrosIntegracaoTest {

    // a mesma chave falsa do application-test.properties (steam.api-key)
    private static final String CHAVE = "CHAVE-DE-TESTE-NAO-PODE-VAZAR";
    // a Steam falsa responde com este corpo: ele também não pode ir para o log nem para a resposta
    private static final String CORPO_DA_STEAM = "CORPO-DA-STEAM-NAO-PODE-VAZAR";
    private static final String URL_BIBLIOTECA = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/";

    // Troca o RestClient da aplicação (AppConfig) por um ligado à Steam falsa, só neste teste.
    // @Primary: o SteamService recebe este, e não o do AppConfig (que continua igual).
    // A ordem importa: o servidor falso precisa ser ligado ao builder ANTES do build().
    @TestConfiguration
    static class SteamFalsa {
        private final RestClient.Builder builder = RestClient.builder();
        private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();

        @Bean
        MockRestServiceServer steamFalsa() {
            return servidor;
        }

        @Bean
        @Primary
        RestClient restClientComSteamFalsa() {
            return builder.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRestServiceServer steamFalsa;

    private Cookie login;

    @BeforeEach
    void preparar() throws Exception {
        steamFalsa.reset();
        login = cadastrarELogarComSteamId("steam-erros@email.com");
    }

    @Test
    void steamForaDoAr503_responde502SemVazarChaveNemCorpo(CapturedOutput saida) throws Exception {
        steamFalsaResponde(HttpStatus.SERVICE_UNAVAILABLE);

        String resposta = importar()
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("A Steam está com problemas"))
                .andExpect(jsonPath("$.detail").value("A Steam respondeu com erro. Tente de novo em alguns minutos."))
                .andReturn().getResponse().getContentAsString();

        // o log aconteceu (senão o teste abaixo passaria "à toa"), só com o status
        assertThat(saida.getAll()).contains("Serviço externo respondeu com erro HTTP 503");
        semVazamento(saida, resposta);
        steamFalsa.verify();
    }

    @Test
    void limiteDaSteam429NaImportacao_responde502SemVazarChaveNemCorpo(CapturedOutput saida) throws Exception {
        steamFalsaResponde(HttpStatus.TOO_MANY_REQUESTS);

        String resposta = importar()
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("A Steam respondeu com erro. Tente de novo em alguns minutos."))
                .andReturn().getResponse().getContentAsString();

        assertThat(saida.getAll()).contains("Serviço externo respondeu com erro HTTP 429");
        semVazamento(saida, resposta);
        steamFalsa.verify();
    }

    @Test
    void chaveRecusada401e403_continuam422ENaoViram502() throws Exception {
        steamFalsaResponde(HttpStatus.UNAUTHORIZED);
        steamFalsaResponde(HttpStatus.FORBIDDEN);

        // o catch do SteamService pega antes do handler novo do GlobalExceptionHandler
        for (int i = 0; i < 2; i++) {
            importar()
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("A Steam recusou a API key")));
        }
        steamFalsa.verify();
    }

    @Test
    void steamNaoResponde_timeout504SemVazarAChave(CapturedOutput saida) throws Exception {
        // a mensagem deste erro traz a URL da requisição ("I/O error on GET request for ..."). Nesta versão do
        // Spring ela vem sem a query (sem a chave), mas o handler do 504 não loga nada mesmo assim
        steamFalsa.expect(requestTo(org.hamcrest.Matchers.startsWith(URL_BIBLIOTECA)))
                .andExpect(queryParam("key", CHAVE))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        String resposta = importar()
                .andExpect(status().isGatewayTimeout())
                .andReturn().getResponse().getContentAsString();

        semVazamento(saida, resposta);
        steamFalsa.verify();
    }

    // ===== Ajudantes =====

    // a Steam falsa espera UMA chamada ao GetOwnedGames COM a chave na URL (prova que havia o que vazar)
    // e responde com o status pedido e o corpo marcado
    private void steamFalsaResponde(HttpStatus status) {
        steamFalsa.expect(requestTo(org.hamcrest.Matchers.startsWith(URL_BIBLIOTECA)))
                .andExpect(queryParam("key", CHAVE))
                .andRespond(withStatus(status).contentType(MediaType.TEXT_PLAIN).body(CORPO_DA_STEAM));
    }

    private org.springframework.test.web.servlet.ResultActions importar() throws Exception {
        return mockMvc.perform(post("/steam/importacao").cookie(login));
    }

    // Nem o log nem a resposta têm NADA da chamada à Steam: a chave, "key=" (o pedaço da URL), o corpo cru
    // da resposta, nem o endereço da API. O endereço entra na conta porque, nesta versão do Spring, a mensagem
    // do timeout traz a URL SEM a query (sem a chave); se uma versão futura voltar a trazer a query, a chave
    // vaza junto. Com esta regra, o teste pega quem logar a exceção, em qualquer versão.
    private static void semVazamento(CapturedOutput saida, String resposta) {
        assertThat(saida.getAll()).doesNotContain(CHAVE).doesNotContain("key=").doesNotContain(CORPO_DA_STEAM)
                .doesNotContain("api.steampowered.com");
        assertThat(resposta).doesNotContain(CHAVE).doesNotContain("key=").doesNotContain(CORPO_DA_STEAM)
                .doesNotContain("api.steampowered.com");
    }

    // cadastra (com Steam ID, senão a importação para antes, no 422 "Cadastre o seu Steam ID"),
    // faz login e devolve o cookie de login
    private Cookie cadastrarELogarComSteamId(String email) throws Exception {
        String senha = "senha-forte-123";
        mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Teste\",\"email\":\"" + email + "\",\"senha\":\"" + senha
                                + "\",\"steamId\":\"76561198000000000\"}"))
                .andExpect(status().isCreated());
        Cookie cookie = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("token");
        assertThat(cookie).isNotNull();
        return new Cookie(cookie.getName(), cookie.getValue());
    }
}
