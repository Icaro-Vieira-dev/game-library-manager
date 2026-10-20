package com.gamelibrary.gamelibrarymanager.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.gamelibrary.gamelibrarymanager.dto.FiltroJogosDTO;
import com.gamelibrary.gamelibrarymanager.dto.JogoPatchDTO;
import com.gamelibrary.gamelibrarymanager.dto.JogoRequestDTO;
import com.gamelibrary.gamelibrarymanager.dto.JogoResponseDTO;
import com.gamelibrary.gamelibrarymanager.dto.PaginaResponseDTO;
import com.gamelibrary.gamelibrarymanager.exception.RequisicaoInvalidaException;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.dto.ConquistaResponseDTO;
import com.gamelibrary.gamelibrarymanager.dto.PlatinaRequestDTO;
import com.gamelibrary.gamelibrarymanager.model.Conquista;
import com.gamelibrary.gamelibrarymanager.service.JogoService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@Tag(name = "Jogos", description = "Cadastro, busca, platina, conquistas e categorias dos jogos")
@RestController
@RequestMapping("/jogos")
public class JogoController {
    private final JogoService jogoService;

    @Autowired
    public JogoController(JogoService jogoService) {
        this.jogoService = jogoService;
    }

    /**
     * GET /jogos
     * Lista os jogos, com filtros opcionais (podem ser combinados) e paginação.
     *
     * Filtros (todos opcionais):
     *   nome          -> parte do nome, sem diferenciar maiúsculas ("ring" acha "Elden Ring")
     *   genero        -> gênero exato, sem diferenciar maiúsculas ("rpg" acha "RPG")
     *   plataforma    -> plataforma exata, sem diferenciar maiúsculas
     *   status        -> JOGANDO, FINALIZADO, ABANDONADO ou PLANEJADO
     *   anoLancamento -> ano exato
     *   categoria     -> nome da categoria, sem diferenciar maiúsculas
     * Paginação:
     *   page -> número da página, começando em 0 (padrão 0)
     *   size -> itens por página (padrão 20, máximo 100)
     *   sort -> ordenação, ex.: sort=horasJogadas,desc (padrão: nomeJogo em ordem alfabética)
     *
     * Exemplos:
     *   GET http://localhost:8080/jogos                                   -> primeira página, 20 jogos
     *   GET http://localhost:8080/jogos?genero=rpg&status=JOGANDO         -> RPGs que você está jogando
     *   GET http://localhost:8080/jogos?nome=resident&page=0&size=5
     *   GET http://localhost:8080/jogos?sort=horasJogadas,desc&size=10     -> os 10 mais jogados
     * Resposta: 200 com { conteudo, pagina, tamanho, totalElementos, totalPaginas, ultima }.
     *           400 se status ou anoLancamento tiver um valor inválido.
     */
    @GetMapping
    public PaginaResponseDTO<JogoResponseDTO> listarJogos(
            @ParameterObject FiltroJogosDTO filtro,
            @ParameterObject @PageableDefault(size = 20, sort = "nomeJogo") Pageable paginacao) {
        Page<Jogo> pagina = jogoService.buscar(filtro, paginacao);
        return PaginaResponseDTO.de(pagina, JogoResponseDTO::deJogo);
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
     * Regras: nome, gênero, plataforma e status são obrigatórios.
     *         Opcionais: anoLancamentoJogo (de 1958 até o ano atual + 5), notaJogo (de 1 a 10),
     *         desenvolvedoraJogo. Sem ano ou sem nota, o jogo fica com 0 nesses campos.
     *         horasJogadas pode ser 0 (jogo ainda não jogado), mas não negativo.
     *         statusJogo aceita: JOGANDO, FINALIZADO, ABANDONADO, PLANEJADO.
     * Resposta: 200 com o jogo criado (já com idJogo). 400 com a lista de campos inválidos.
     */
    @PostMapping
    public JogoResponseDTO criar(@Valid @RequestBody JogoRequestDTO dto) {
        Jogo jogo = dto.toJogo();
        Jogo jogosalvo = jogoService.criar(jogo);
        return JogoResponseDTO.deJogo(jogosalvo);
    }

    /**
     * GET /jogos/{id}
     * Busca um jogo pelo id.
     *
     * Exemplo: GET http://localhost:8080/jogos/5
     * Resposta: 200 com o jogo. 404 se não existir. 400 se o id não for um número.
     */
    @GetMapping("/{id}")
    public JogoResponseDTO buscarPorId(@PathVariable Long id) {
       Jogo jogo = jogoService.buscarPorId(id);
       return JogoResponseDTO.deJogo(jogo);
    }

    /**
     * PATCH /jogos/{id}
     * Atualiza SÓ os campos enviados; o resto do jogo fica como está.
     * Use este para mudanças pontuais (dar nota, trocar o status...). O PUT substitui tudo.
     *
     * Exemplos: PATCH http://localhost:8080/jogos/5
     *   Dar nota:              { "notaJogo": 9.5 }
     *   Marcar como zerado:    { "statusJogo": "FINALIZADO" }
     *   Várias coisas juntas:  { "generoJogo": "Metroidvania", "plataformaJogo": "Switch" }
     * Regras: as mesmas do POST, mas nenhum campo é obrigatório. Texto enviado não pode ser vazio.
     * Resposta: 200 com o jogo atualizado. 404 se não existir. 400 se algum campo enviado for inválido.
     */
    @PatchMapping("/{id}")
    public JogoResponseDTO atualizarParcialmente(@PathVariable Long id, @Valid @RequestBody JogoPatchDTO mudancas) {
        return JogoResponseDTO.deJogo(jogoService.atualizarParcialmente(id, mudancas));
    }

    /**
     * PUT /jogos/{id}
     * Atualiza um jogo. Usa o mesmo body e as mesmas regras do POST /jogos.
     * Atenção: substitui TODOS os campos. Um campo que não for enviado fica vazio (ou 0) no banco.
     * Para mudar só um ou outro campo, use o PATCH /jogos/{id}.
     *
     * Exemplo: PUT http://localhost:8080/jogos/5   (body igual ao do POST)
     * Resposta: 200 com o jogo atualizado. 404 se não existir. 400 se algum campo for inválido.
     */
    @PutMapping("/{id}")
    public JogoResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody JogoRequestDTO jogo) {
        Jogo request = jogo.toJogo();
        Jogo jogoatualizado = jogoService.atualizar(id, request);
        return JogoResponseDTO.deJogo(jogoatualizado);
    }

    /**
     * DELETE /jogos/{id}
     * Apaga um jogo.
     *
     * Exemplo: DELETE http://localhost:8080/jogos/5
     * Resposta: 204 (sem corpo) se apagou. 404 se não existir.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id) {
        jogoService.apagar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /jogos?confirmar=true
     * Apaga TODOS os jogos, inclusive os cadastrados à mão. Não tem como desfazer.
     * O ?confirmar=true é obrigatório: é uma trava contra um DELETE /jogos mandado por engano
     * (por exemplo, esquecendo o id no fim da URL).
     *
     * Exemplo: DELETE http://localhost:8080/jogos?confirmar=true
     * Resposta: 204 (sem corpo). 400 se faltar o ?confirmar=true (nesse caso nada é apagado).
     */
    @DeleteMapping
    public ResponseEntity<Void> apagarTodos(@RequestParam(defaultValue = "false") boolean confirmar) {
        if (!confirmar) {
            throw new RequisicaoInvalidaException("Para apagar TODOS os jogos, confirme com DELETE /jogos?confirmar=true. Não tem como desfazer.");
        }
        jogoService.apagarTodos();
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
        Jogo jogo = jogoService.definirPlatina(id, dto.toPlatina());
        return ResponseEntity.ok(JogoResponseDTO.deJogo(jogo));

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
        Jogo jogo = jogoService.removerPlatina(id);
        return ResponseEntity.ok(JogoResponseDTO.deJogo(jogo));
    }

    /**
     * PUT /jogos/{id}/categorias/{categoriaId}
     * Liga o jogo a uma categoria. Um jogo pode ter várias categorias.
     * Se o jogo já estava nessa categoria, nada muda (não duplica).
     *
     * Exemplo: PUT http://localhost:8080/jogos/5/categorias/2   (sem body)
     * Resposta: 200 com o jogo, já com a categoria em "categorias".
     *           404 se o jogo ou a categoria não existir.
     */
    @PutMapping("/{id}/categorias/{categoriaId}")
    public JogoResponseDTO vincularCategoria(@PathVariable Long id, @PathVariable Long categoriaId) {
        return JogoResponseDTO.deJogo(jogoService.vincularCategoria(id, categoriaId));
    }

    /**
     * DELETE /jogos/{id}/categorias/{categoriaId}
     * Tira o jogo de uma categoria. O jogo e a categoria NÃO são apagados, só o vínculo.
     *
     * Exemplo: DELETE http://localhost:8080/jogos/5/categorias/2
     * Resposta: 200 com o jogo, já sem essa categoria. 404 se o jogo ou a categoria não existir.
     */
    @DeleteMapping("/{id}/categorias/{categoriaId}")
    public JogoResponseDTO desvincularCategoria(@PathVariable Long id, @PathVariable Long categoriaId) {
        return JogoResponseDTO.deJogo(jogoService.desvincularCategoria(id, categoriaId));
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
        for (Conquista conquista : jogoService.listarConquistas(id, concluida)) {
            lista.add(ConquistaResponseDTO.deConquista(conquista));
        }
        return lista;
    }
}
