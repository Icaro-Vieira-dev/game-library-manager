package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.dto.CategoryRequestDTO;
import com.gamelibrary.gamelibrarymanager.dto.CategoryResponseDTO;
import com.gamelibrary.gamelibrarymanager.exception.CategoriaNaoEncontrada;
import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.service.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/categorias")
public class CategoryController {
    private final CategoriaService categoriaService;

    @Autowired
    public CategoryController(CategoriaService categoriaService) {this.categoriaService = categoriaService;}

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

    @PostMapping
    public CategoryResponseDTO create(@RequestBody CategoryRequestDTO dtoCategoria){
        Categoria categoria = dtoCategoria.toCategoria();
        Categoria categoriaResponse = categoriaService.createCategoria(categoria);
        return CategoryResponseDTO.fromCategory(categoriaResponse);
    }

    @PostMapping("/manyCategories")
    public List<CategoryResponseDTO> creatMany(@RequestBody List<String> categorias){
        List<Categoria> novasCategorias = categoriaService.creatManyCategories(categorias);
        List<CategoryResponseDTO> listaCategoria = new ArrayList<>();
        for (Categoria categoria : novasCategorias) {
            listaCategoria.add(CategoryResponseDTO.fromCategory(categoria));
        }
        return listaCategoria;

    }
    @GetMapping("/{id}")
    public CategoryResponseDTO findId(@PathVariable Long id){
        Categoria categoria = categoriaService.findCategoriaById(id);
        return CategoryResponseDTO.fromCategory(categoria);
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id){
        categoriaService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/deleteAll")
    public ResponseEntity<Void> deleteAll(){
        categoriaService.deleteAll();
        return ResponseEntity.noContent().build();
    }






}
