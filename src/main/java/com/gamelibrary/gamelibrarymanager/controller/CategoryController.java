package com.gamelibrary.gamelibrarymanager.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.gamelibrary.gamelibrarymanager.dto.CategoryRequestDTO;
import com.gamelibrary.gamelibrarymanager.dto.CategoryResponseDTO;
import com.gamelibrary.gamelibrarymanager.exception.CategoriaNaoEncontrada;
import com.gamelibrary.gamelibrarymanager.model.Categoria;
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
public class CategoryController {
    private final CategoriaService categoriaService;

    @Autowired
    public CategoryController(CategoriaService categoriaService) {this.categoriaService = categoriaService;}

    /**
     * GET /categorias
     * Lista todas as categorias, cada uma com a lista dos seus jogos.
     * Categorias são os gêneros: as da Steam são criadas pelo POST /importSteam/detalhes.
     *
     * Exemplo: GET http://localhost:8080/categorias
     * Resposta: 200 com a lista ([] se não houver categorias).
     */
    @GetMapping
    public List<CategoryResponseDTO> findAll(){
        List<Categoria> categorias = categoriaService.findAllCategoria();
        List<CategoryResponseDTO> listaCategoria = new ArrayList<>();

        for (Categoria categoria : categorias) {
            CategoryResponseDTO categoriaResponse = CategoryResponseDTO.fromCategory(categoria);
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
    public CategoryResponseDTO create(@Valid @RequestBody CategoryRequestDTO dtoCategoria){
        Categoria categoria = dtoCategoria.toCategoria();
        Categoria categoriaResponse = categoriaService.createCategoria(categoria);
        return CategoryResponseDTO.fromCategory(categoriaResponse);
    }

    /**
     * POST /categorias/manyCategories
     * Cria várias categorias de uma vez. O body é uma lista de nomes (não de objetos).
     *
     * Exemplo: POST http://localhost:8080/categorias/manyCategories
     * Body (JSON): ["RPG", "Ação", "Estratégia"]
     * Nomes que já existem (ou repetidos na lista) são reaproveitados, sem duplicar.
     * Resposta: 200 com as categorias (novas e já existentes), sem repetição.
     *           400 se a lista estiver vazia ou tiver algum nome vazio (nesse caso nada é criado).
     */
    @PostMapping("/manyCategories")
    public List<CategoryResponseDTO> creatMany(@RequestBody List<String> categorias){
        List<Categoria> novasCategorias = categoriaService.creatManyCategories(categorias);
        List<CategoryResponseDTO> listaCategoria = new ArrayList<>();
        for (Categoria categoria : novasCategorias) {
            listaCategoria.add(CategoryResponseDTO.fromCategory(categoria));
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
    public CategoryResponseDTO findId(@PathVariable Long id){
        Categoria categoria = categoriaService.findCategoriaById(id);
        return CategoryResponseDTO.fromCategory(categoria);
    }

    /**
     * GET /categorias/nome?nome=...
     * Lista as categorias cujo nome CONTÉM o texto, sem diferenciar maiúsculas: "rp" encontra "RPG".
     *
     * Exemplo: GET http://localhost:8080/categorias/nome?nome=rp
     * Resposta: 200 com a lista ([] se não achar nada).
     */
    @GetMapping("/nome")
    public List<CategoryResponseDTO> findByNome(@RequestParam String nome){
        List<Categoria> categorias = categoriaService.findByName(nome);
        List<CategoryResponseDTO> listaCategoria = new ArrayList<>();
        for (Categoria categoria : categorias) {
            CategoryResponseDTO categoriaResponse = CategoryResponseDTO.fromCategory(categoria);
            listaCategoria.add(categoriaResponse);
        }
        return listaCategoria;

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
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id){
        categoriaService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /categorias/deleteAll
     * Apaga TODAS as categorias. Não tem como desfazer.
     *
     * Exemplo: DELETE http://localhost:8080/categorias/deleteAll
     * Resposta: 204 (sem corpo).
     *           409 se qualquer jogo estiver em alguma categoria (nesse caso nada é apagado).
     */
    @DeleteMapping("/deleteAll")
    public ResponseEntity<Void> deleteAll(){
        categoriaService.deleteAll();
        return ResponseEntity.noContent().build();
    }






}
