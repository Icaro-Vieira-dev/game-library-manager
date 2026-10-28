package com.gamelibrary.gamelibrarymanager.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Teste de INTEGRAÇÃO do cabeçalho Cache-Control: só o GET /vitrine (público e igual para todos) pode
// ser guardado em cache; todas as outras rotas continuam com o no-store padrão do Spring Security.
// Passa pela cadeia inteira do Spring Security, que é quem escreve o no-store.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CacheControlIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void vitrine_temCachePublicoDe5Minutos() throws Exception {
        // nos testes a vitrine está vazia (agendamento desligado), mas o cabeçalho sai do mesmo jeito
        mockMvc.perform(get("/vitrine"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("public")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=300")))
                // o Spring Security não somou o no-store dele, nem os outros cabeçalhos "não guarde"
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, not(containsString("no-store"))))
                .andExpect(header().doesNotExist(HttpHeaders.PRAGMA))
                .andExpect(header().doesNotExist(HttpHeaders.EXPIRES))
                // nenhum cookie numa resposta que um proxy pode guardar e entregar para outras pessoas
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void vitrine_vistaPeloFront_variaPorOrigin() throws Exception {
        // um cache compartilhado (proxy/CDN) não pode entregar os cabeçalhos CORS de uma origem para outra:
        // o Vary: Origin manda o cache guardar uma cópia por origem
        mockMvc.perform(get("/vitrine").header(HttpHeaders.ORIGIN, "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("public")))
                .andExpect(header().stringValues(HttpHeaders.VARY, hasItem(containsString("Origin"))));
    }

    @Test
    void jogos_continuaSemCache() throws Exception {
        mockMvc.perform(get("/jogos"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, not(containsString("public"))));
    }

    @Test
    void usuariosMe_continuaSemCache() throws Exception {
        Cookie login = cadastrarELogar("cache-me@email.com");

        mockMvc.perform(get("/usuarios/me").cookie(login))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, not(containsString("public"))));
    }

    // cadastra, faz login e devolve o cookie de login
    private Cookie cadastrarELogar(String email) throws Exception {
        String senha = "senha-forte-123";
        mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Teste\",\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isCreated());
        Cookie cookie = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("token");
        assertThat(cookie).isNotNull();
        return new Cookie(cookie.getName(), cookie.getValue());
    }
}
