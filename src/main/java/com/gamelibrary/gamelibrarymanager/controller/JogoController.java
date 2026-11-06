package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.UsuarioLogado;
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
import com.gamelibrary.gamelibrarymanager.model.Usuario;
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


// TODAS as rotas daqui exigem login e só enxergam os jogos do USUÁRIO LOGADO (o @UsuarioLogado vem do
// token, nunca da requisição). Jogo de outra pessoa responde o mesmo 404 de "não existe".
// Sem login, ou com token inválido: 401 em todas.
@Tag(name = "Jogos", description = "Os jogos do usuário logado: cadastro, busca, platina, conquistas e categorias (exige login)")
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
     * Lista os jogos DO USUÁRIO LOGADO, com filtros opcionais (podem ser combinados) e paginação.
     * Exige login.
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
     *           400 se status ou anoLancamento tiver um valor inválido. 401 sem login.
     */
    @GetMapping
    public PaginaResponseDTO<JogoResponseDTO> listarJogos(
            @ParameterObject FiltroJogosDTO filtro,
            @ParameterObject @PageableDefault(size = 20, sort = "nomeJogo") Pageable paginacao,
            @UsuarioLogado Usuario usuario) {
        Page<Jogo> pagina = jogoService.buscar(filtro, paginacao, usuario);
        return PaginaResponseDTO.de(pagina, JogoResponseDTO::deJogo);
    }

    /**
     * POST /jogos
     * Cadastra um jogo manualmente na biblioteca DO USUÁRIO LOGADO (ele é o dono). Exige login.
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
     * Resposta: 200 com o jogo criado (já com idJogo). 400 com a lista de campos inválidos. 401 sem login.
     */
    @PostMapping
    public JogoResponseDTO criar(@Valid @RequestBody JogoRequestDTO dto, @UsuarioLogado Usuario usuario) {
        Jogo jogo = dto.toJogo();
        Jogo jogosalvo = jogoService.criar(jogo, usuario);
        return JogoResponseDTO.deJogo(jogosalvo);
    }

    /**
     * GET /jogos/{id}
     * Busca um jogo DO USUÁRIO LOGADO pelo id. Exige login.
     *
     * Exemplo: GET http://localhost:8080/jogos/5
     * Resposta: 200 com o jogo. 404 se não existir OU for de outra pessoa (a mesma resposta nos dois casos).
     *           400 se o id não for um número. 401 sem login.
     */
    @GetMapping("/{id}")
    public JogoResponseDTO buscarPorId(@PathVariable Long id, @UsuarioLogado Usuario usuario) {
       Jogo jogo = jogoService.buscarPorId(id, usuario);
       return JogoResponseDTO.deJogo(jogo);
    }

    /**
     * PATCH /jogos/{id}
     * Atualiza SÓ os campos enviados; o resto do jogo fica como está. Só jogos DO USUÁRIO LOGADO.
     * Use este para mudanças pontuais (dar nota, trocar o status...). O PUT substitui tudo.
     *
     * Exemplos: PATCH http://localhost:8080/jogos/5
     *   Dar nota:              { "notaJogo": 9.5 }
     *   Marcar como zerado:    { "statusJogo": "FINALIZADO" }
     *   Várias coisas juntas:  { "generoJogo": "Metroidvania", "plataformaJogo": "Switch" }
     * Regras: as mesmas do POST, mas nenhum campo é obrigatório. Texto enviado não pode ser vazio.
     * Resposta: 200 com o jogo atualizado. 404 se não existir ou for de outra pessoa.
     *           400 se algum campo enviado for inválido. 401 sem login.
     */
    @PatchMapping("/{id}")
    public JogoResponseDTO atualizarParcialmente(@PathVariable Long id, @Valid @RequestBody JogoPatchDTO mudancas,
                                                 @UsuarioLogado Usuario usuario) {
        return JogoResponseDTO.deJogo(jogoService.atualizarParcialmente(id, mudancas, usuario));
    }

    /**
     * PUT /jogos/{id}
     * Atualiza um jogo DO USUÁRIO LOGADO. Usa o mesmo body e as mesmas regras do POST /jogos.
     * Atenção: substitui TODOS os campos. Um campo que não for enviado fica vazio (ou 0) no banco.
     * Para mudar só um ou outro campo, use o PATCH /jogos/{id}.
     *
     * Exemplo: PUT http://localhost:8080/jogos/5   (body igual ao do POST)
     * Resposta: 200 com o jogo atualizado. 404 se não existir ou for de outra pessoa.
     *           400 se algum campo for inválido. 401 sem login.
     */
    @PutMapping("/{id}")
    public JogoResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody JogoRequestDTO jogo,
                                     @UsuarioLogado Usuario usuario) {
        Jogo request = jogo.toJogo();
        Jogo jogoatualizado = jogoService.atualizar(id, request, usuario);
        return JogoResponseDTO.deJogo(jogoatualizado);
    }

    /**
     * DELETE /jogos/{id}
     * Apaga um jogo DO USUÁRIO LOGADO.
     *
     * Exemplo: DELETE http://localhost:8080/jogos/5
     * Resposta: 204 (sem corpo) se apagou. 404 se não existir ou for de outra pessoa. 401 sem login.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id, @UsuarioLogado Usuario usuario) {
        jogoService.apagar(id, usuario);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /jogos?confirmar=true
     * Apaga TODOS os jogos DO USUÁRIO LOGADO, inclusive os cadastrados à mão. Não tem como desfazer.
     * Os jogos das outras pessoas não são tocados.
     * O ?confirmar=true é obrigatório: é uma trava contra um DELETE /jogos mandado por engano
     * (por exemplo, esquecendo o id no fim da URL).
     *
     * Exemplo: DELETE http://localhost:8080/jogos?confirmar=true
     * Resposta: 204 (sem corpo). 400 se faltar o ?confirmar=true (nesse caso nada é apagado). 401 sem login.
     */
    @DeleteMapping
    public ResponseEntity<Void> apagarTodos(@RequestParam(defaultValue = "false") boolean confirmar,
                                            @UsuarioLogado Usuario usuario) {
        if (!confirmar) {
            throw new RequisicaoInvalidaException("Para apagar TODOS os jogos, confirme com DELETE /jogos?confirmar=true. Não tem como desfazer.");
        }
        jogoService.apagarTodos(usuario);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /jogos/{id}/platina
     * Define a platina de um jogo DO USUÁRIO LOGADO: a meta de platinar e o progresso de conquistas.
     * A platina é opcional: jogos sem platina definida não mostram o campo "platina".
     *
     * Jogo importado da Steam: mande só { "platinaAtiva": true } e o progresso é buscado na Steam, com o
     * Steam ID do usuário logado (e o checklist em GET /jogos/{id}/conquistas é atualizado).
     * Para atualizar depois, chame de novo.
     *
     * Exemplos: PUT http://localhost:8080/jogos/5/platina
     *   Ativar (Steam busca os números):  { "platinaAtiva": true }
     *   Informar à mão (ex.: jogo de PS5): { "platinaAtiva": true, "conquistaObtida": 45, "totalConquista": 60 }
     *   Desistir da meta:                  { "platinaAtiva": false }   (mantém os números já salvos)
     * Regras: platinaAtiva é obrigatório; conquistaObtida e totalConquista vão juntos ou nenhum;
     *         obtidas entre 0 e o total; total maior que 0. Números enviados no body têm prioridade sobre a Steam.
     * Resposta: 200 com o jogo, incluindo "conquistaRestante" e "platinado" (calculados).
     *           400 se algum campo for inválido. 404 se o jogo não existir ou for de outra pessoa.
     *           422 se o jogo não tiver conquistas na Steam, suas conquistas estiverem privadas
     *               ou você não tiver Steam ID cadastrado. 401 sem login.
     */
    @PutMapping("/{id}/platina")
    public ResponseEntity<JogoResponseDTO> definirPlatina(@PathVariable Long id, @Valid @RequestBody PlatinaRequestDTO dto,
                                                          @UsuarioLogado Usuario usuario) {
        Jogo jogo = jogoService.definirPlatina(id, dto.toPlatina(), usuario);
        return ResponseEntity.ok(JogoResponseDTO.deJogo(jogo));

    }

    /**
     * DELETE /jogos/{id}/platina
     * Remove a platina de um jogo DO USUÁRIO LOGADO (volta a ficar sem platina definida) e apaga o
     * checklist de conquistas. O jogo não é apagado.
     *
     * Exemplo: DELETE http://localhost:8080/jogos/5/platina
     * Resposta: 200 com o jogo, já sem o campo "platina". 404 se o jogo não existir ou for de outra pessoa.
     *           401 sem login.
     */
    @DeleteMapping("/{id}/platina")
    public ResponseEntity<JogoResponseDTO> removerPlatina(@PathVariable Long id, @UsuarioLogado Usuario usuario) {
        Jogo jogo = jogoService.removerPlatina(id, usuario);
        return ResponseEntity.ok(JogoResponseDTO.deJogo(jogo));
    }

    /**
     * PUT /jogos/{id}/categorias/{categoriaId}
     * Liga um jogo a uma categoria. O jogo E a categoria precisam ser DO USUÁRIO LOGADO.
     * Um jogo pode ter várias categorias. Se o jogo já estava nessa categoria, nada muda (não duplica).
     *
     * Exemplo: PUT http://localhost:8080/jogos/5/categorias/2   (sem body)
     * Resposta: 200 com o jogo, já com a categoria em "categorias".
     *           404 se o jogo ou a categoria não existir ou for de outra pessoa. 401 sem login.
     */
    @PutMapping("/{id}/categorias/{categoriaId}")
    public JogoResponseDTO vincularCategoria(@PathVariable Long id, @PathVariable Long categoriaId,
                                             @UsuarioLogado Usuario usuario) {
        return JogoResponseDTO.deJogo(jogoService.vincularCategoria(id, categoriaId, usuario));
    }

    /**
     * DELETE /jogos/{id}/categorias/{categoriaId}
     * Tira um jogo DO USUÁRIO LOGADO de uma categoria dele. O jogo e a categoria NÃO são apagados, só o vínculo.
     *
     * Exemplo: DELETE http://localhost:8080/jogos/5/categorias/2
     * Resposta: 200 com o jogo, já sem essa categoria.
     *           404 se o jogo ou a categoria não existir ou for de outra pessoa. 401 sem login.
     */
    @DeleteMapping("/{id}/categorias/{categoriaId}")
    public JogoResponseDTO desvincularCategoria(@PathVariable Long id, @PathVariable Long categoriaId,
                                                @UsuarioLogado Usuario usuario) {
        return JogoResponseDTO.deJogo(jogoService.desvincularCategoria(id, categoriaId, usuario));
    }

    /**
     * GET /jogos/{id}/conquistas
     * Checklist de conquistas de um jogo DO USUÁRIO LOGADO, preenchido pela Steam ao ativar a platina
     * (PUT /jogos/{id}/platina).
     *
     * Exemplos:
     *   GET http://localhost:8080/jogos/5/conquistas                  -> todas
     *   GET http://localhost:8080/jogos/5/conquistas?concluida=false  -> só as que faltam
     *   GET http://localhost:8080/jogos/5/conquistas?concluida=true   -> só as já feitas
     * Resposta: 200 com a lista ([] se a platina ainda não foi sincronizada com a Steam).
     *           404 se o jogo não existir ou for de outra pessoa. 401 sem login.
     */
    @GetMapping("/{id}/conquistas")
    public List<ConquistaResponseDTO> listarConquistas(@PathVariable Long id, @RequestParam(required = false) Boolean concluida,
                                                       @UsuarioLogado Usuario usuario) {
        List<ConquistaResponseDTO> lista = new ArrayList<>();
        for (Conquista conquista : jogoService.listarConquistas(id, concluida, usuario)) {
            lista.add(ConquistaResponseDTO.deConquista(conquista));
        }
        return lista;
    }
}
