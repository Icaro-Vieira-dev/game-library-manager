package com.gamelibrary.gamelibrarymanager.controller;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Teste de INTEGRAÇÃO do isolamento entre usuários: cada um só vê e mexe nos PRÓPRIOS jogos e categorias.
// Dois usuários (A e B) logados de verdade (cadastro + login, cookie), passando pela cadeia inteira do
// Spring Security. Os nomes dos testes dizem a regra que eles provam.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IsolamentoUsuariosIntegracaoTest {

    private static final String JOGO_DE_A =
            "{\"nomeJogo\":\"Jogo da Ana\",\"generoJogo\":\"RPG\",\"plataformaJogo\":\"PC\",\"statusJogo\":\"JOGANDO\"}";

    @Autowired
    private MockMvc mockMvc;

    // a) as rotas de jogos agora exigem login
    @Test
    void semLogin_jogos401() throws Exception {
        mockMvc.perform(get("/jogos"))
                .andExpect(status().isUnauthorized());
    }

    // b) B não enxerga o jogo de A: nem pelo id (404, igual a "não existe"), nem na listagem
    @Test
    void outroUsuario_naoVeOJogoDeA_nemPeloIdNemNaLista() throws Exception {
        Cookie a = cadastrarELogar("ana@email.com");
        Cookie b = cadastrarELogar("bruno@email.com");
        Integer idDoJogoDeA = criarJogo(a);

        mockMvc.perform(get("/jogos/" + idDoJogoDeA).cookie(b))
                .andExpect(status().isNotFound())
                // a MESMA resposta de um id que não existe: B não descobre que o id é de alguém
                .andExpect(jsonPath("$.detail").value("Jogo do id: " + idDoJogoDeA + " Não encontrado"));

        mockMvc.perform(get("/jogos").cookie(b))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.conteudo[*].nomeJogo", not(hasItem("Jogo da Ana"))));

        // e A continua vendo o próprio jogo
        mockMvc.perform(get("/jogos/" + idDoJogoDeA).cookie(a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeJogo").value("Jogo da Ana"));
    }

    // c) o "apagar todos" de B só apaga os jogos de B
    @Test
    void apagarTodosDeB_naoApagaOsJogosDeA() throws Exception {
        Cookie a = cadastrarELogar("ana-apagar@email.com");
        Cookie b = cadastrarELogar("bruno-apagar@email.com");
        criarJogo(a);

        mockMvc.perform(delete("/jogos").param("confirmar", "true").cookie(b))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/jogos").cookie(a))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[*].nomeJogo").value(contains("Jogo da Ana")));
    }

    // d) um cookie vencido ou adulterado não derruba uma rota pública (o token nem é lido nela)
    @Test
    void vitrine_comCookieInvalido_200() throws Exception {
        mockMvc.perform(get("/vitrine").cookie(new Cookie("token", "token-vencido-ou-adulterado")))
                .andExpect(status().isOk());
    }

    // Categorias são por usuário: "RPG" e "rpg" são o mesmo nome para o MESMO usuário (409),
    // mas outra pessoa pode ter o seu próprio "RPG"
    @Test
    void categoria_mesmoNomeComOutraCaixa_409ParaOMesmoUsuario_eLivreParaOutro() throws Exception {
        Cookie a = cadastrarELogar("ana-categorias@email.com");
        Cookie b = cadastrarELogar("bruno-categorias@email.com");

        mockMvc.perform(post("/categorias").cookie(a).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"RPG\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/categorias").cookie(a).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"rpg\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Categoria já existe"));

        mockMvc.perform(post("/categorias").cookie(b).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"RPG\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("RPG"));
    }

    // ===== Ajudantes =====

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

    // cria o "Jogo da Ana" com o login recebido e devolve o id dele
    private Integer criarJogo(Cookie login) throws Exception {
        String resposta = mockMvc.perform(post("/jogos").cookie(login).contentType(MediaType.APPLICATION_JSON).content(JOGO_DE_A))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.idJogo");
    }
}
