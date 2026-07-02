package com.gamelibrary.gamelibrarymanager.service;
import com.gamelibrary.gamelibrarymanager.exception.CategoriaNaoEncontrada;
import com.gamelibrary.gamelibrarymanager.exception.JogoNaoDeletadoException;
import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoriaService{
    private final CategoriaRepository repositoryCategoria;

    @Autowired
    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.repositoryCategoria = categoriaRepository;
    }

    public Categoria createCategoria(Categoria categoria) {
        return repositoryCategoria.save(categoria);
    }

    public List<Categoria> findAllCategoria() {
        return repositoryCategoria.findAll();
    }

    public Categoria findCategoriaById(Long id) {
        return repositoryCategoria.findById(id). orElseThrow(() -> new CategoriaNaoEncontrada(id));
    }

    public List<Categoria> findByName(String nome) {
        String nomeTratado = nome.toLowerCase().strip();
        return repositoryCategoria.findByNomeContainingIgnoreCase(nomeTratado);
    }

    public boolean deleteCategory(Long id) {
        if(!(repositoryCategoria.existsById(id))){
            throw new CategoriaNaoEncontrada(id);
        }
        repositoryCategoria.deleteById(id);
        return true;
    }

    public boolean deleteAll() {
        repositoryCategoria.deleteAll();
        return true;
    }

    public List<Categoria> creatManyCategories(List<String> nomeAll) {
        List<Categoria> categorias = new ArrayList<>();
        for (String nomeCategoria : nomeAll) {
           Categoria categoria = new Categoria(nomeCategoria);
           categorias.add(repositoryCategoria.save(categoria));
        }
        return categorias;

    }
}
