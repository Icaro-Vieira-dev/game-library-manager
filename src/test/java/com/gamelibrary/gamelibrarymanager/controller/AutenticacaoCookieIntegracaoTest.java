package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.SegurancaConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Teste de INTEGRAÇÃO do login por cookie httpOnly: sobe a aplicação inteira (banco H2 do perfil "test")
// e faz requisições com o MockMvc, passando por TODA a cadeia do Spring Security (FiltroOrigem, CORS, JWT).
//
// Cada teste usa um e-mail diferente: o limite de tentativas de login fica em memória e vale para a
// aplicação inteira, então um teste não pode atrapalhar o outro.
// @Transactional: os usuários criados em cada teste são desfeitos no final.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AutenticacaoCookieIntegracaoTest {

    private static final String SENHA = "senha-forte-123";
    // a CORS_ORIGEM do perfil de teste (o padrão do application.properties)
    private static final String ORIGEM_DO_FRONT = "http://localhost:4200";
    private static final String ORIGEM_MALICIOSA = "http://site-malicioso.com";

    @Autowired
    private MockMvc mockMvc;

    // ===== Login devolve o token no cookie, e nunca no corpo =====

    @Test
    void login_devolveCookieHttpOnlyEOCorpoSoComExpiraEm() throws Exception {
        cadastrar("login-cookie@email.com");

        MockHttpServletResponse resposta = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(jsonLogin("login-cookie@email.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiraEm").exists())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.tipo").doesNotExist())
                .andReturn().getResponse();

        MockCookie cookie = (MockCookie) resposta.getCookie("token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getMaxAge()).isPositive();
        // o token não aparece em lugar nenhum do corpo
        assertThat(resposta.getContentAsString()).doesNotContain(cookie.getValue());
    }

    @Test
    void login_comUmCookieInvalidoJunto_funcionaNormalmente() throws Exception {
        cadastrar("cookie-velho@email.com");

        // sem a regra "/auth/** não autentica", este cookie faria o próprio login responder 401
        mockMvc.perform(post("/auth/login").cookie(new Cookie("token", "token-velho-ou-adulterado"))
                        .contentType(MediaType.APPLICATION_JSON).content(jsonLogin("cookie-velho@email.com")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.startsWith("token=ey")));
    }

    // ===== GET /usuarios/me: cookie ou cabeçalho =====

    @Test
    void me_comOCookie_200() throws Exception {
        String token = cadastrarELogar("me-cookie@email.com");

        mockMvc.perform(get("/usuarios/me").cookie(new Cookie("token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me-cookie@email.com"));
    }

    @Test
    void me_comOCabecalhoAuthorization_200() throws Exception {
        String token = cadastrarELogar("me-cabecalho@email.com");

        mockMvc.perform(get("/usuarios/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me-cabecalho@email.com"));
    }

    @Test
    void me_semNada_401SemMexerEmCookie() throws Exception {
        mockMvc.perform(get("/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(SegurancaConfig.MENSAGEM_NAO_AUTENTICADO))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void me_comCookieInvalido_401EMandaApagarOCookie() throws Exception {
        MockHttpServletResponse resposta = mockMvc.perform(get("/usuarios/me").cookie(new Cookie("token", "adulterado")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(SegurancaConfig.MENSAGEM_NAO_AUTENTICADO))
                .andReturn().getResponse();

        MockCookie apagar = (MockCookie) resposta.getCookie("token");
        assertThat(apagar).isNotNull();
        assertThat(apagar.getMaxAge()).isZero();
        assertThat(apagar.getValue()).isEmpty();
        // mesmos atributos do cookie de login: sem isso o navegador não apagaria
        assertThat(apagar.isHttpOnly()).isTrue();
        assertThat(apagar.getSameSite()).isEqualTo("Strict");
        assertThat(apagar.getPath()).isEqualTo("/");
    }

    // ===== Logout =====

    @Test
    void logout_204EMandaApagarOCookie() throws Exception {
        MockHttpServletResponse resposta = mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        MockCookie apagar = (MockCookie) resposta.getCookie("token");
        assertThat(apagar).isNotNull();
        assertThat(apagar.getMaxAge()).isZero();
        assertThat(apagar.isHttpOnly()).isTrue();
        assertThat(apagar.getSecure()).isTrue();
        assertThat(apagar.getSameSite()).isEqualTo("Strict");
        assertThat(apagar.getPath()).isEqualTo("/");
    }

    // ===== FiltroOrigem (proteção contra CSRF) =====

    @Test
    void origemMaliciosa_noLogin_403() throws Exception {
        cadastrar("csrf-login@email.com");

        mockMvc.perform(post("/auth/login").header(HttpHeaders.ORIGIN, ORIGEM_MALICIOSA)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonLogin("csrf-login@email.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Origem não permitida."))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void origemMaliciosa_numaRotaPublicaQueNaoEhDeLogin_403ENadaEhCriado() throws Exception {
        // prova que o FiltroOrigem protege a API inteira, e não só o /auth
        mockMvc.perform(post("/categorias").header(HttpHeaders.ORIGIN, ORIGEM_MALICIOSA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Categoria do Atacante\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Origem não permitida."));

        mockMvc.perform(get("/categorias"))
                .andExpect(jsonPath("$[*].nome", not(hasItem("Categoria do Atacante"))));
    }

    @Test
    void origemDoFront_passa() throws Exception {
        cadastrar("origem-front@email.com");

        mockMvc.perform(post("/auth/login").header(HttpHeaders.ORIGIN, ORIGEM_DO_FRONT)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonLogin("origem-front@email.com")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DO_FRONT))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void origemDaPropriaApi_passa() throws Exception {
        // o Swagger roda na própria API; no MockMvc a API é http://localhost (porta 80, que o navegador omite)
        mockMvc.perform(post("/categorias").header(HttpHeaders.ORIGIN, "http://localhost")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Pelo Swagger\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void semOrigin_passa() throws Exception {
        // cliente fora do navegador (Postman, curl): não sofre CSRF
        mockMvc.perform(post("/categorias")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Pelo Postman\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void originNull_403() throws Exception {
        mockMvc.perform(post("/auth/logout").header(HttpHeaders.ORIGIN, "null"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Origem não permitida."));
    }

    @Test
    void origemQueSoComecaIgualAoFront_403() throws Exception {
        // a comparação é exata: um startsWith deixaria passar este domínio do atacante
        mockMvc.perform(post("/auth/logout").header(HttpHeaders.ORIGIN, ORIGEM_DO_FRONT + ".site-malicioso.com"))
                .andExpect(status().isForbidden());
    }

    // ===== CORS com credenciais =====

    @Test
    void preflightDoFront_liberaComCredenciais() throws Exception {
        mockMvc.perform(options("/usuarios/me")
                        .header(HttpHeaders.ORIGIN, ORIGEM_DO_FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DO_FRONT))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void preflightDeOutraOrigem_recusado() throws Exception {
        mockMvc.perform(options("/usuarios/me")
                        .header(HttpHeaders.ORIGIN, ORIGEM_MALICIOSA)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    // ===== Ajudantes =====

    private void cadastrar(String email) throws Exception {
        mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Teste\",\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isCreated());
    }

    // cadastra, faz login e devolve o token tirado do cookie (é de lá que ele vem agora)
    private String cadastrarELogar(String email) throws Exception {
        cadastrar(email);
        Cookie cookie = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(jsonLogin(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("token");
        assertThat(cookie).isNotNull();
        return cookie.getValue();
    }

    private static String jsonLogin(String email) {
        return "{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}";
    }
}
