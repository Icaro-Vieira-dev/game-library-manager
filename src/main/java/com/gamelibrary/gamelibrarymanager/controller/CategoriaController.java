package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.UsuarioLogado;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.gamelibrary.gamelibrarymanager.dto.CategoriaRequestDTO;
import com.gamelibrary.gamelibrarymanager.dto.CategoriaResponseDTO;
import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.exception.RequisicaoInvalidaException;
import com.gamelibrary.gamelibrarymanager.model.Usuario;
import com.gamelibrary.gamelibrarymanager.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

// TODAS as rotas daqui exigem login. Categorias são POR USUÁRIO: o "RPG" de uma pessoa não é o "RPG" de
// outra, e cada um só enxerga as próprias. Categoria de outra pessoa responde o mesmo 404 de "não existe".
// Sem login, ou com token inválido: 401 em todas.
@Tag(name = "Categorias", description = "As categorias (gêneros) do usuário logado e os jogos dele em cada uma (exige login)")
@RestController
@RequestMapping("/categorias")
public class CategoriaController {
    private final CategoriaService categoriaService;

    @Autowired
    public CategoriaController(CategoriaService categoriaService) {this.categoriaService = categoriaService;}

    /**
     * GET /categorias
     * Lista as categorias DO USUÁRIO LOGADO, cada uma com a lista dos jogos DELE nessa categoria.
     * Categorias são os gêneros: as da Steam são criadas pelo POST /steam/detalhes. Exige login.
     *
     * Filtro opcional:
     *   nome -> só as categorias cujo nome CONTÉM o texto, sem diferenciar maiúsculas ("rp" acha "RPG")
     *
     * Exemplos:
     *   GET http://localhost:8080/categorias           -> todas as suas
     *   GET http://localhost:8080/categorias?nome=rp   -> só as suas que têm "rp" no nome
     * Resposta: 200 com a lista ([] se não houver ou não achar nada). 401 sem login.
     */
    @GetMapping
    public List<CategoriaResponseDTO> listar(@RequestParam(required = false) String nome, @UsuarioLogado Usuario usuario){
        List<Categoria> categorias = (nome == null || nome.isBlank())
                ? categoriaService.listarTodas(usuario)
                : categoriaService.buscarPorNome(nome, usuario);
        List<CategoriaResponseDTO> listaCategoria = new ArrayList<>();

        for (Categoria categoria : categorias) {
            CategoriaResponseDTO categoriaResponse = CategoriaResponseDTO.deCategoria(categoria);
            listaCategoria.add(categoriaResponse);
        }
        return listaCategoria;
    }

    /**
     * POST /categorias
     * Cria uma categoria para o USUÁRIO LOGADO (ele é o dono). Exige login.
     *
     * Exemplo: POST http://localhost:8080/categorias
     * Body (JSON): { "nome": "RPG" }
     * Regras: nome obrigatório, até 100 caracteres; espaços nas pontas são removidos.
     * Resposta: 200 com a categoria criada (já com o id).
     *           400 se o nome estiver vazio. 401 sem login.
     *           409 se VOCÊ já tiver uma com esse nome ("rpg" = "RPG"). Outra pessoa pode ter o mesmo nome.
     */
    @PostMapping
    public CategoriaResponseDTO criar(@Valid @RequestBody CategoriaRequestDTO dto, @UsuarioLogado Usuario usuario){
        Categoria categoria = dto.toCategoria(usuario);
        Categoria categoriaResponse = categoriaService.criar(categoria);
        return CategoriaResponseDTO.deCategoria(categoriaResponse);
    }

    /**
     * POST /categorias/lote
     * Cria várias categorias de uma vez para o USUÁRIO LOGADO. O body é uma lista de nomes (não de objetos).
     *
     * Exemplo: POST http://localhost:8080/categorias/lote
     * Body (JSON): ["RPG", "Ação", "Estratégia"]
     * Nomes que você já tem (ou repetidos na lista) são reaproveitados, sem duplicar.
     * Resposta: 200 com as categorias (novas e já existentes), sem repetição.
     *           400 se a lista estiver vazia ou tiver algum nome vazio (nesse caso nada é criado). 401 sem login.
     *           409 se outro pedido criar o mesmo nome no mesmo instante (nesse caso nada é criado; tente de novo).
     */
    @PostMapping("/lote")
    public List<CategoriaResponseDTO> criarVarias(@RequestBody List<String> categorias, @UsuarioLogado Usuario usuario){
        List<Categoria> novasCategorias = categoriaService.criarVarias(categorias, usuario);
        List<CategoriaResponseDTO> listaCategoria = new ArrayList<>();
        for (Categoria categoria : novasCategorias) {
            listaCategoria.add(CategoriaResponseDTO.deCategoria(categoria));
        }
        return listaCategoria;

    }
    /**
     * GET /categorias/{id}
     * Busca uma categoria DO USUÁRIO LOGADO pelo id. Exige login.
     *
     * Exemplo: GET http://localhost:8080/categorias/1
     * Resposta: 200 com a categoria e os seus jogos nela.
     *           404 se não existir OU for de outra pessoa (a mesma resposta nos dois casos). 401 sem login.
     */
    @GetMapping("/{id}")
    public CategoriaResponseDTO buscarPorId(@PathVariable Long id, @UsuarioLogado Usuario usuario){
        Categoria categoria = categoriaService.buscarPorId(id, usuario);
        return CategoriaResponseDTO.deCategoria(categoria);
    }

    /**
     * DELETE /categorias/{id}
     * Apaga uma categoria DO USUÁRIO LOGADO. Exige login.
     *
     * Exemplo: DELETE http://localhost:8080/categorias/1
     * Resposta: 204 (sem corpo) se apagou. 404 se não existir ou for de outra pessoa. 401 sem login.
     *           409 se algum jogo seu estiver nessa categoria (a mensagem diz quantos). Tire os jogos antes
     *           com DELETE /jogos/{id}/categorias/{categoriaId}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id, @UsuarioLogado Usuario usuario){
        categoriaService.apagar(id, usuario);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /categorias?confirmar=true
     * Apaga TODAS as categorias DO USUÁRIO LOGADO. Não tem como desfazer. As das outras pessoas não são tocadas.
     * O ?confirmar=true é obrigatório: é uma trava contra um DELETE /categorias mandado por engano.
     *
     * Exemplo: DELETE http://localhost:8080/categorias?confirmar=true
     * Resposta: 204 (sem corpo). 400 se faltar o ?confirmar=true. 401 sem login.
     *           409 se qualquer jogo seu estiver em alguma categoria (nesse caso nada é apagado).
     */
    @DeleteMapping
    public ResponseEntity<Void> apagarTodas(@RequestParam(defaultValue = "false") boolean confirmar,
                                            @UsuarioLogado Usuario usuario){
        if (!confirmar) {
            throw new RequisicaoInvalidaException("Para apagar TODAS as categorias, confirme com DELETE /categorias?confirmar=true. Não tem como desfazer.");
        }
        categoriaService.apagarTodas(usuario);
        return ResponseEntity.noContent().build();
    }
}
