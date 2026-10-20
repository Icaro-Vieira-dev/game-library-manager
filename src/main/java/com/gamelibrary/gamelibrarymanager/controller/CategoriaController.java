package com.gamelibrary.gamelibrarymanager.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.gamelibrary.gamelibrarymanager.dto.CategoriaRequestDTO;
import com.gamelibrary.gamelibrarymanager.dto.CategoriaResponseDTO;
import com.gamelibrary.gamelibrarymanager.exception.CategoriaNaoEncontradaException;
import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.exception.RequisicaoInvalidaException;
import com.gamelibrary.gamelibrarymanager.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Categorias", description = "Categorias (gêneros) e os jogos de cada uma")
@RestController
@RequestMapping("/categorias")
public class CategoriaController {
    private final CategoriaService categoriaService;

    @Autowired
    public CategoriaController(CategoriaService categoriaService) {this.categoriaService = categoriaService;}

    /**
     * GET /categorias
     * Lista as categorias, cada uma com a lista dos seus jogos.
     * Categorias são os gêneros: as da Steam são criadas pelo POST /steam/detalhes.
     *
     * Filtro opcional:
     *   nome -> só as categorias cujo nome CONTÉM o texto, sem diferenciar maiúsculas ("rp" acha "RPG")
     *
     * Exemplos:
     *   GET http://localhost:8080/categorias           -> todas
     *   GET http://localhost:8080/categorias?nome=rp   -> só as que têm "rp" no nome
     * Resposta: 200 com a lista ([] se não houver ou não achar nada).
     */
    @GetMapping
    public List<CategoriaResponseDTO> listar(@RequestParam(required = false) String nome){
        List<Categoria> categorias = (nome == null || nome.isBlank())
                ? categoriaService.listarTodas()
                : categoriaService.buscarPorNome(nome);
        List<CategoriaResponseDTO> listaCategoria = new ArrayList<>();

        for (Categoria categoria : categorias) {
            CategoriaResponseDTO categoriaResponse = CategoriaResponseDTO.deCategoria(categoria);
            listaCategoria.add(categoriaResponse);
        }
        return listaCategoria;
    }

    /**
     * POST /categorias
     * Cria uma categoria.
     *
     * Exemplo: POST http://localhost:8080/categorias
     * Body (JSON): { "nome": "RPG" }
     * Regras: nome obrigatório, até 100 caracteres; espaços nas pontas são removidos.
     * Resposta: 200 com a categoria criada (já com o id).
     *           400 se o nome estiver vazio. 409 se já existir uma com esse nome ("rpg" = "RPG").
     */
    @PostMapping
    public CategoriaResponseDTO criar(@Valid @RequestBody CategoriaRequestDTO dto){
        Categoria categoria = dto.toCategoria();
        Categoria categoriaResponse = categoriaService.criar(categoria);
        return CategoriaResponseDTO.deCategoria(categoriaResponse);
    }

    /**
     * POST /categorias/lote
     * Cria várias categorias de uma vez. O body é uma lista de nomes (não de objetos).
     *
     * Exemplo: POST http://localhost:8080/categorias/lote
     * Body (JSON): ["RPG", "Ação", "Estratégia"]
     * Nomes que já existem (ou repetidos na lista) são reaproveitados, sem duplicar.
     * Resposta: 200 com as categorias (novas e já existentes), sem repetição.
     *           400 se a lista estiver vazia ou tiver algum nome vazio (nesse caso nada é criado).
     */
    @PostMapping("/lote")
    public List<CategoriaResponseDTO> criarVarias(@RequestBody List<String> categorias){
        List<Categoria> novasCategorias = categoriaService.criarVarias(categorias);
        List<CategoriaResponseDTO> listaCategoria = new ArrayList<>();
        for (Categoria categoria : novasCategorias) {
            listaCategoria.add(CategoriaResponseDTO.deCategoria(categoria));
        }
        return listaCategoria;

    }
    /**
     * GET /categorias/{id}
     * Busca uma categoria pelo id.
     *
     * Exemplo: GET http://localhost:8080/categorias/1
     * Resposta: 200 com a categoria e seus jogos. 404 se não existir.
     */
    @GetMapping("/{id}")
    public CategoriaResponseDTO buscarPorId(@PathVariable Long id){
        Categoria categoria = categoriaService.buscarPorId(id);
        return CategoriaResponseDTO.deCategoria(categoria);
    }

    /**
     * DELETE /categorias/{id}
     * Apaga uma categoria.
     *
     * Exemplo: DELETE http://localhost:8080/categorias/1
     * Resposta: 204 (sem corpo) se apagou. 404 se não existir.
     *           409 se algum jogo estiver nessa categoria (a mensagem diz quantos). Tire os jogos antes
     *           com DELETE /jogos/{id}/categorias/{categoriaId}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id){
        categoriaService.apagar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /categorias?confirmar=true
     * Apaga TODAS as categorias. Não tem como desfazer.
     * O ?confirmar=true é obrigatório: é uma trava contra um DELETE /categorias mandado por engano.
     *
     * Exemplo: DELETE http://localhost:8080/categorias?confirmar=true
     * Resposta: 204 (sem corpo). 400 se faltar o ?confirmar=true.
     *           409 se qualquer jogo estiver em alguma categoria (nesse caso nada é apagado).
     */
    @DeleteMapping
    public ResponseEntity<Void> apagarTodas(@RequestParam(defaultValue = "false") boolean confirmar){
        if (!confirmar) {
            throw new RequisicaoInvalidaException("Para apagar TODAS as categorias, confirme com DELETE /categorias?confirmar=true. Não tem como desfazer.");
        }
        categoriaService.apagarTodas();
        return ResponseEntity.noContent().build();
    }






}
