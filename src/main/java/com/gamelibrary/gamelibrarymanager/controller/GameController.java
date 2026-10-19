package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.dto.JogoRequestDTO;
import com.gamelibrary.gamelibrarymanager.dto.JogoResponseDTO;
import com.gamelibrary.gamelibrarymanager.exception.JogoNaoEncontradoException;
import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.dto.ConquistaResponseDTO;
import com.gamelibrary.gamelibrarymanager.dto.PlatinaRequestDTO;
import com.gamelibrary.gamelibrarymanager.model.Conquista;
import com.gamelibrary.gamelibrarymanager.service.GameService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;


@RestController
@RequestMapping("/jogos")
public class GameController {
    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * GET /jogos
     * Lista todos os jogos cadastrados.
     *
     * Exemplo: GET http://localhost:8080/jogos
     * Resposta: 200 com a lista ([] se não houver jogos).
     */
    @GetMapping
    public List<JogoResponseDTO> listarJogos() {
        List<Jogo> jogos = gameService.listarTodos();
        List<JogoResponseDTO> listaJogos = new ArrayList<>();

        for (Jogo jogo : jogos) {
            JogoResponseDTO responseDTO = JogoResponseDTO.fromJogo(jogo);
            listaJogos.add(responseDTO);

        }
        return listaJogos;
    }

    /**
     * POST /jogos
     * Cadastra um jogo manualmente.
     *
     * Exemplo: POST http://localhost:8080/jogos
     * Body (JSON):
     *   {
     *     "nomeJogo": "Elden Ring",
     *     "generoJogo": "RPG",
     *     "plataformaJogo": "PC",
     *     "anoLancamentoJogo": 2022,
     *     "horasJogadas": 120.5,
     *     "statusJogo": "FINALIZADO",
     *     "notaJogo": 10,
     *     "desenvolvedoraJogo": "FromSoftware"
     *   }
     * Regras: nome, gênero, plataforma e status são obrigatórios; ano entre 1958 e 2026;
     *         horasJogadas maior que 0 (zero é recusado); nota de 1 a 10; desenvolvedora é opcional.
     *         statusJogo aceita: JOGANDO, FINALIZADO, ABANDONADO, PLANEJADO.
     * Resposta: 200 com o jogo criado (já com idJogo). 400 com a lista de campos inválidos.
     */
    @PostMapping
    public JogoResponseDTO criarJogos(@Valid @RequestBody JogoRequestDTO dto) {
        Jogo jogo = dto.toJogo();
        Jogo jogosalvo = gameService.criarJogo(jogo);
        return JogoResponseDTO.fromJogo(jogosalvo);
    }

    /**
     * GET /jogos/{id}
     * Busca um jogo pelo id.
     *
     * Exemplo: GET http://localhost:8080/jogos/5
     * Resposta: 200 com o jogo. 404 se não existir. 400 se o id não for um número.
     */
    @GetMapping("/{id}")
    public JogoResponseDTO buscaID(@Valid @PathVariable Long id) {
       Jogo jogo = gameService.buscarPorId(id);
       return JogoResponseDTO.fromJogo(jogo);
    }

    /**
     * PUT /jogos/{id}
     * Atualiza um jogo. Usa o mesmo body e as mesmas regras do POST /jogos.
     * Atenção: substitui TODOS os campos. Um campo que não for enviado fica vazio (ou 0) no banco.
     *
     * Exemplo: PUT http://localhost:8080/jogos/5   (body igual ao do POST)
     * Resposta: 200 com o jogo atualizado. 404 se não existir. 400 se algum campo for inválido.
     */
    @PutMapping("/{id}")
    public JogoResponseDTO atualizarJogo(@PathVariable Long id,@Valid @RequestBody JogoRequestDTO jogo) {
        Jogo request = jogo.toJogo();
        Jogo jogoatualizado = gameService.atualizarJogo(id, request);
        return JogoResponseDTO.fromJogo(jogoatualizado);
    }

    /**
     * DELETE /jogos/{id}
     * Apaga um jogo.
     *
     * Exemplo: DELETE http://localhost:8080/jogos/5
     * Resposta: 204 (sem corpo) se apagou. 404 se não existir.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirJogo(@PathVariable Long id) {
        gameService.deleteJogo(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /jogos/deleteAllJogos
     * Apaga TODOS os jogos, inclusive os cadastrados à mão. Não tem como desfazer.
     *
     * Exemplo: DELETE http://localhost:8080/jogos/deleteAllJogos
     * Cuidado com o caminho: "/jogos/deleteAll" ou uma barra no final caem em outra rota e dão 400/404.
     * Resposta: 204 (sem corpo).
     */
    @DeleteMapping("/deleteAllJogos")
    public ResponseEntity<Void> deleteAllJogos() {
        gameService.deleteAll();
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /jogos/{id}/platina
     * Define a platina de um jogo: a meta de platinar e o progresso de conquistas.
     * A platina é opcional: jogos sem platina definida não mostram o campo "platina".
     *
     * Jogo importado da Steam: mande só { "platinaAtiva": true } e o progresso é buscado na Steam
     * (e o checklist em GET /jogos/{id}/conquistas é atualizado). Para atualizar depois, chame de novo.
     *
     * Exemplos: PUT http://localhost:8080/jogos/5/platina
     *   Ativar (Steam busca os números):  { "platinaAtiva": true }
     *   Informar à mão (ex.: jogo de PS5): { "platinaAtiva": true, "conquistaObtida": 45, "totalConquista": 60 }
     *   Desistir da meta:                  { "platinaAtiva": false }   (mantém os números já salvos)
     * Regras: platinaAtiva é obrigatório; conquistaObtida e totalConquista vão juntos ou nenhum;
     *         obtidas entre 0 e o total; total maior que 0. Números enviados no body têm prioridade sobre a Steam.
     * Resposta: 200 com o jogo, incluindo "conquistaRestante" e "platinado" (calculados).
     *           400 se algum campo for inválido. 404 se o jogo não existir.
     *           422 se o jogo não tiver conquistas na Steam ou suas conquistas estiverem privadas.
     */
    @PutMapping("/{id}/platina")
    public ResponseEntity<JogoResponseDTO> definirPlatina(@PathVariable Long id, @Valid @RequestBody PlatinaRequestDTO dto) {
        Jogo jogo = gameService.definirPlatina(id, dto.toPlatina());
        return ResponseEntity.ok(JogoResponseDTO.fromJogo(jogo));

    }

    /**
     * DELETE /jogos/{id}/platina
     * Remove a platina do jogo (volta a ficar sem platina definida) e apaga o checklist de conquistas.
     * O jogo não é apagado.
     *
     * Exemplo: DELETE http://localhost:8080/jogos/5/platina
     * Resposta: 200 com o jogo, já sem o campo "platina". 404 se o jogo não existir.
     */
    @DeleteMapping("/{id}/platina")
    public ResponseEntity<JogoResponseDTO> removerPlatina(@PathVariable Long id) {
        Jogo jogo = gameService.removerPlatina(id);
        return ResponseEntity.ok(JogoResponseDTO.fromJogo(jogo));
    }

    /**
     * GET /jogos/{id}/conquistas
     * Checklist de conquistas do jogo, preenchido pela Steam ao ativar a platina (PUT /jogos/{id}/platina).
     *
     * Exemplos:
     *   GET http://localhost:8080/jogos/5/conquistas                  -> todas
     *   GET http://localhost:8080/jogos/5/conquistas?concluida=false  -> só as que faltam
     *   GET http://localhost:8080/jogos/5/conquistas?concluida=true   -> só as já feitas
     * Resposta: 200 com a lista ([] se a platina ainda não foi sincronizada com a Steam). 404 se o jogo não existir.
     */
    @GetMapping("/{id}/conquistas")
    public List<ConquistaResponseDTO> listarConquistas(@PathVariable Long id, @RequestParam(required = false) Boolean concluida) {
        List<ConquistaResponseDTO> lista = new ArrayList<>();
        for (Conquista conquista : gameService.listarConquistas(id, concluida)) {
            lista.add(ConquistaResponseDTO.fromConquista(conquista));
        }
        return lista;
    }

    /**
     * GET /jogos/buscar/genero?genero=...
     * Lista os jogos de um gênero. A busca é EXATA e diferencia maiúsculas: "RPG" não encontra "rpg".
     *
     * Exemplo: GET http://localhost:8080/jogos/buscar/genero?genero=RPG
     * Resposta: 200 com a lista ([] se não achar nada).
     */
    @GetMapping("buscar/genero")
    public List<JogoResponseDTO>buscarPorGenero(@Valid @RequestParam String genero) {
        List<Jogo> generos = gameService.buscarGenero(genero);
        List<JogoResponseDTO> listaGeneros = new ArrayList<>();

        for (Jogo jogo : generos) {
            listaGeneros.add(JogoResponseDTO.fromJogo(jogo));
        }
        return listaGeneros;
    }

    /**
     * GET /jogos/buscar/plataforma?plataforma=...
     * Lista os jogos de uma plataforma. A busca é EXATA e diferencia maiúsculas: "PC" não encontra "pc".
     *
     * Exemplo: GET http://localhost:8080/jogos/buscar/plataforma?plataforma=PC
     * Resposta: 200 com a lista ([] se não achar nada).
     */
    @GetMapping("buscar/plataforma")
    public List<JogoResponseDTO>buscarPorPLataforma(@RequestParam String plataforma) {
        List<Jogo> plataformas = gameService.buscarJogoPorPlataforma(plataforma);
        List<JogoResponseDTO> listaPlataformas = new ArrayList<>();
        for (Jogo jogo : plataformas) {
            listaPlataformas.add(JogoResponseDTO.fromJogo(jogo));
        }
        return listaPlataformas;
    }

    /**
     * GET /jogos/buscar/nome?nome=...
     * Lista os jogos cujo nome CONTÉM o texto, sem diferenciar maiúsculas: "ring" encontra "Elden Ring".
     *
     * Exemplo: GET http://localhost:8080/jogos/buscar/nome?nome=ring
     * Resposta: 200 com a lista ([] se não achar nada).
     */
    @GetMapping("buscar/nome")
    public List<JogoResponseDTO> buscarPorNome(@RequestParam String nome) {
        List<Jogo> nomes = gameService.buscarJogoPorNome(nome);
        List<JogoResponseDTO> listaNomes = new ArrayList<>();
        for (Jogo jogo : nomes) {
            listaNomes.add(JogoResponseDTO.fromJogo(jogo));
        }
        return listaNomes;
    }
    /**
     * GET /jogos/buscar/anoLancamento?anoLancamento=...
     * Lista os jogos lançados em um ano.
     *
     * Exemplo: GET http://localhost:8080/jogos/buscar/anoLancamento?anoLancamento=2022
     * Resposta: 200 com a lista. 400 se o ano não for um número.
     */
    @GetMapping("buscar/anoLancamento")
    public List<JogoResponseDTO>buscarPorAnoLancamento(@RequestParam int anoLancamento) {
        List<Jogo> lancaento = gameService.buscarJogoPorAnoLancamento(anoLancamento);
        List<JogoResponseDTO> listaLancamentos = new ArrayList<>();
        for (Jogo jogo : lancaento) {
            listaLancamentos.add(JogoResponseDTO.fromJogo(jogo));
        }
        return listaLancamentos;

    }

    /**
     * GET /jogos/buscar/status?status=...
     * Lista os jogos com um status. Valores aceitos (em maiúsculas): JOGANDO, FINALIZADO, ABANDONADO, PLANEJADO.
     *
     * Exemplo: GET http://localhost:8080/jogos/buscar/status?status=JOGANDO
     * Resposta: 200 com a lista. 400 se o status não for um dos valores aceitos.
     */
    @GetMapping("buscar/status")
    public List<JogoResponseDTO>buscarPorStatus(@RequestParam GameStatus status) {
        List<Jogo> jogoStatus = gameService.buscarJogoPorStatus(status);
        List<JogoResponseDTO> listaJogos = new ArrayList<>();
        for (Jogo jogo : jogoStatus) {
            listaJogos.add(JogoResponseDTO.fromJogo(jogo));
        }
        return listaJogos;
    }
}
