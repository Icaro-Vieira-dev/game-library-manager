package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.model.StatusJogo;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.repository.CategoriaRepository;
import com.gamelibrary.gamelibrarymanager.repository.JogoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Teste de INTEGRAÇÃO: sobe a aplicação inteira (com o banco H2 do perfil "test") e faz
// requisições HTTP "de mentira" com o MockMvc, sem abrir porta. Testa controller + service +
// repositório + banco juntos, do jeito que um cliente da API (Postman, front-end) usaria.
//
// @Transactional: cada teste roda dentro de uma transação que é DESFEITA no final,
// então os jogos criados no @BeforeEach não "vazam" de um teste para o outro.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JogoApiIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JogoRepository jogoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @BeforeEach
    void criarJogos() {
        Categoria rpg = categoriaRepository.save(new Categoria("RPG"));
        Categoria acao = categoriaRepository.save(new Categoria("Ação"));

        Jogo elden = new Jogo("Elden Ring", StatusJogo.FINALIZADO, "RPG", "PC", "FromSoftware", 2022, 80, 10);
        elden.adicionarCategoria(rpg);
        elden.adicionarCategoria(acao);
        jogoRepository.save(elden);

        Jogo hades = new Jogo("Hades", StatusJogo.JOGANDO, "Ação", "PC", "Supergiant", 2020, 40, 9);
        hades.adicionarCategoria(acao);
        jogoRepository.save(hades);

        jogoRepository.save(new Jogo("Zelda: Tears of the Kingdom", StatusJogo.PLANEJADO, "Aventura", "Switch", "Nintendo", 2023, 0, 0));
    }

    // ===== Item 6: busca unificada com paginação =====

    @Test
    void semFiltros_trazTodosEmOrdemAlfabetica() throws Exception {
        mockMvc.perform(get("/jogos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.conteudo[*].nomeJogo").value(contains("Elden Ring", "Hades", "Zelda: Tears of the Kingdom")));
    }

    @Test
    void generoSemDiferenciarMaiusculas() throws Exception {
        mockMvc.perform(get("/jogos").param("genero", "rpg"))
                .andExpect(jsonPath("$.conteudo[*].nomeJogo").value(contains("Elden Ring")));
    }

    @Test
    void filtrosCombinados_generoEStatus() throws Exception {
        mockMvc.perform(get("/jogos").param("genero", "ação").param("status", "JOGANDO"))
                .andExpect(jsonPath("$.conteudo[*].nomeJogo").value(contains("Hades")));
    }

    @Test
    void nomeContendoParteDoTexto() throws Exception {
        mockMvc.perform(get("/jogos").param("nome", "ZELDA"))
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].plataformaJogo").value("Switch"));
    }

    @Test
    void porCategoria_semRepetirJogo() throws Exception {
        // Elden Ring tem 2 categorias; o distinct garante que ele aparece uma vez só
        mockMvc.perform(get("/jogos").param("categoria", "AÇÃO"))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.conteudo[*].nomeJogo").value(contains("Elden Ring", "Hades")));
    }

    @Test
    void ordenarPorHorasDecrescente() throws Exception {
        mockMvc.perform(get("/jogos").param("sort", "horasJogadas,desc"))
                .andExpect(jsonPath("$.conteudo[*].nomeJogo").value(contains("Elden Ring", "Hades", "Zelda: Tears of the Kingdom")));
    }

    @Test
    void paginacao_segundaPaginaDeTamanhoDois() throws Exception {
        mockMvc.perform(get("/jogos").param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    @Test
    void tamanhoDePaginaLimitadoA100() throws Exception {
        mockMvc.perform(get("/jogos").param("size", "500"))
                .andExpect(jsonPath("$.tamanho").value(100));
    }

    @Test
    void statusInvalido_responde400() throws Exception {
        mockMvc.perform(get("/jogos").param("status", "XPTO"))
                .andExpect(status().isBadRequest());
    }

    // ===== Item 7: erros no padrão ProblemDetail =====

    @Test
    void jogoInexistente_404NoFormatoProblemDetail() throws Exception {
        mockMvc.perform(get("/jogos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Jogo não encontrado"))
                .andExpect(jsonPath("$.detail").value("Jogo do id: 999999 Não encontrado"))
                .andExpect(jsonPath("$.instance").value("/jogos/999999"));
    }

    @Test
    void validacao_listaOsCamposInvalidosEmErros() throws Exception {
        mockMvc.perform(post("/jogos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeJogo\":\"\",\"generoJogo\":\"RPG\",\"plataformaJogo\":\"PC\",\"statusJogo\":\"JOGANDO\",\"notaJogo\":11}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.nomeJogo").value("O campo nome não pode estar vazio"))
                .andExpect(jsonPath("$.erros.notaJogo").value("A nota deve ser entre 1 e 10."));
    }

    @Test
    void easterEggDoFuturoContinuaLa() throws Exception {
        mockMvc.perform(post("/jogos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeJogo\":\"X\",\"generoJogo\":\"RPG\",\"plataformaJogo\":\"PC\",\"statusJogo\":\"JOGANDO\",\"anoLancamentoJogo\":2999}"))
                .andExpect(jsonPath("$.erros.anoLancamentoNoLimite").value("Está prevendo o futuro amigão ?"));
    }

    // pegou um bug real na conversão para records (item 10): sem "horasJogadas", o Jackson 3
    // recusava o JSON, porque o campo era um double primitivo
    @Test
    void cadastrarSemHorasJogadas_ficaComZero() throws Exception {
        mockMvc.perform(post("/jogos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeJogo\":\"Hollow Knight\",\"generoJogo\":\"Metroidvania\",\"plataformaJogo\":\"PC\",\"statusJogo\":\"PLANEJADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasJogadas").value(0.0))
                .andExpect(jsonPath("$.notaJogo").value(0.0));
    }

    @Test
    void jsonMalformado_400() throws Exception {
        mockMvc.perform(post("/jogos").contentType(MediaType.APPLICATION_JSON).content("{ isso não é json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
    }

    @Test
    void ordenacaoPorCampoInexistente_400() throws Exception {
        mockMvc.perform(get("/jogos").param("sort", "campoQueNaoExiste"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ordenação inválida"));
    }

    @Test
    void rotaInexistente_continua404EnaoVira500() throws Exception {
        mockMvc.perform(get("/rota-que-nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void metodoNaoPermitido_405() throws Exception {
        mockMvc.perform(post("/jogos/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    // ===== Item 8: PATCH muda só o que for enviado =====

    @Test
    void patch_mudaSoANotaEPreservaOResto() throws Exception {
        Long id = idDe("Hades");
        mockMvc.perform(patch("/jogos/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"notaJogo\": 7.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notaJogo").value(7.5))
                // tudo o que não foi enviado continua igual
                .andExpect(jsonPath("$.nomeJogo").value("Hades"))
                .andExpect(jsonPath("$.generoJogo").value("Ação"))
                .andExpect(jsonPath("$.statusJogo").value("JOGANDO"))
                .andExpect(jsonPath("$.horasJogadas").value(40.0))
                .andExpect(jsonPath("$.categorias").value(contains("Ação")));
    }

    @Test
    void patch_variosCamposDeUmaVez() throws Exception {
        Long id = idDe("Zelda: Tears of the Kingdom");
        mockMvc.perform(patch("/jogos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusJogo\":\"JOGANDO\",\"horasJogadas\":12}"))
                .andExpect(jsonPath("$.statusJogo").value("JOGANDO"))
                .andExpect(jsonPath("$.horasJogadas").value(12.0))
                .andExpect(jsonPath("$.plataformaJogo").value("Switch"));
    }

    @Test
    void patch_textoVazioEhRecusado() throws Exception {
        Long id = idDe("Hades");
        mockMvc.perform(patch("/jogos/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"nomeJogo\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nomeJogo").value("O campo nome não pode estar vazio"));
    }

    @Test
    void patch_jogoInexistente_404() throws Exception {
        mockMvc.perform(patch("/jogos/999999").contentType(MediaType.APPLICATION_JSON).content("{\"notaJogo\": 5}"))
                .andExpect(status().isNotFound());
    }

    // ===== Item 10: os records com @AssertTrue continuam validando =====

    @Test
    void platinaManual_comNumeros() throws Exception {
        Long id = idDe("Zelda: Tears of the Kingdom");   // jogo sem steamAppId: não chama a Steam
        mockMvc.perform(put("/jogos/" + id + "/platina").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platinaAtiva\":true,\"conquistaObtida\":45,\"totalConquista\":60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platina.conquistaRestante").value(15))
                .andExpect(jsonPath("$.platina.platinado").value(false));
    }

    @Test
    void platina_obtidasMaiorQueTotal_400() throws Exception {
        Long id = idDe("Zelda: Tears of the Kingdom");
        mockMvc.perform(put("/jogos/" + id + "/platina").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platinaAtiva\":true,\"conquistaObtida\":70,\"totalConquista\":60}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.obtidasDentroDoTotal").value("conquistaObtida não pode ser maior que totalConquista."));
    }

    // ===== Item 13: rotas REST =====

    @Test
    void apagarTodosOsJogos_semConfirmar_400ENadaEhApagado() throws Exception {
        mockMvc.perform(delete("/jogos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Para apagar TODOS os jogos, confirme com DELETE /jogos?confirmar=true. Não tem como desfazer."));
        mockMvc.perform(get("/jogos")).andExpect(jsonPath("$.totalElementos").value(3));
    }

    @Test
    void apagarTodosOsJogos_comConfirmar_204() throws Exception {
        mockMvc.perform(delete("/jogos").param("confirmar", "true"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/jogos")).andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void categorias_filtroPorNomeNaListagem() throws Exception {
        mockMvc.perform(get("/categorias").param("nome", "rp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome").value(contains("RPG")));
    }

    @Test
    void categorias_criarEmLote() throws Exception {
        mockMvc.perform(post("/categorias/lote").contentType(MediaType.APPLICATION_JSON).content("[\"Terror\", \"rpg\"]"))
                .andExpect(status().isOk())
                // "Terror" é criada; "rpg" reaproveita a "RPG" que já existe
                .andExpect(jsonPath("$[*].nome").value(contains("Terror", "RPG")));
    }

    @Test
    void apagarTodasAsCategorias_semConfirmar_400() throws Exception {
        mockMvc.perform(delete("/categorias"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rotasAntigas_naoExistemMais() throws Exception {
        // dois pedaços depois de /jogos: não bate com /jogos/{id} (que é um pedaço só) -> rota inexistente
        mockMvc.perform(get("/jogos/buscar/genero").param("genero", "RPG")).andExpect(status().isNotFound());
        // um pedaço só: cai em /categorias/{id}, e "nome" não é um número -> parâmetro inválido
        mockMvc.perform(get("/categorias/nome").param("nome", "rp")).andExpect(status().isBadRequest());
        // bate com /categorias/{id}, que existe, mas só aceita GET e DELETE -> método não permitido
        mockMvc.perform(post("/categorias/manyCategories").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/importSteam/importar")).andExpect(status().isNotFound());
    }

    private Long idDe(String nome) {
        return jogoRepository.findAll().stream()
                .filter(jogo -> jogo.getNomeJogo().equals(nome))
                .findFirst().orElseThrow().getIdJogo();
    }
}
