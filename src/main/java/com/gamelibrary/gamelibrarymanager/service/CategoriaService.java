package com.gamelibrary.gamelibrarymanager.service;
import com.gamelibrary.gamelibrarymanager.exception.CategoriaDuplicadaException;
import com.gamelibrary.gamelibrarymanager.exception.CategoriaEmUsoException;
import com.gamelibrary.gamelibrarymanager.exception.CategoriaNaoEncontrada;
import com.gamelibrary.gamelibrarymanager.exception.RequisicaoInvalidaException;
import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.repository.CategoriaRepository;
import com.gamelibrary.gamelibrarymanager.repository.JogoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoriaService{
    private final CategoriaRepository repositoryCategoria;
    private final JogoRepository jogoRepository;

    @Autowired
    public CategoriaService(CategoriaRepository categoriaRepository, JogoRepository jogoRepository) {
        this.repositoryCategoria = categoriaRepository;
        this.jogoRepository = jogoRepository;
    }

    // não deixa criar duas categorias com o mesmo nome ("RPG" e "rpg" contam como iguais)
    public Categoria createCategoria(Categoria categoria) {
        repositoryCategoria.findFirstByNomeIgnoreCase(categoria.getNome()).ifPresent(existente -> {
            throw new CategoriaDuplicadaException(existente.getNome(), existente.getId());
        });
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

    // só apaga categoria sem jogos; com jogos, responde 409 dizendo quantos
    public boolean deleteCategory(Long id) {
        if(!(repositoryCategoria.existsById(id))){
            throw new CategoriaNaoEncontrada(id);
        }
        long jogosNaCategoria = jogoRepository.countByCategorias_Id(id);
        if (jogosNaCategoria > 0) {
            throw new CategoriaEmUsoException("A categoria " + id + " tem " + jogosNaCategoria + " jogo(s). "
                    + "Tire os jogos dela antes (DELETE /jogos/{id}/categorias/" + id + ").");
        }
        repositoryCategoria.deleteById(id);
        return true;
    }

    // mesma regra: se qualquer categoria tiver jogos, não apaga nenhuma
    public boolean deleteAll() {
        long vinculos = jogoRepository.contarVinculosComCategorias();
        if (vinculos > 0) {
            throw new CategoriaEmUsoException("Existem " + vinculos + " vínculo(s) entre jogos e categorias. "
                    + "Tire os jogos das categorias antes de apagar todas.");
        }
        repositoryCategoria.deleteAll();
        return true;
    }

    // usado pela Steam: reaproveita a categoria se já existir (sem diferenciar maiúsculas), senão cria
    public Categoria buscarOuCriarPorNome(String nome) {
        return repositoryCategoria.findFirstByNomeIgnoreCase(nome.strip())
                .orElseGet(() -> repositoryCategoria.save(new Categoria(nome.strip())));
    }

    // Cria várias de uma vez. Primeiro valida TODOS os nomes (se um for inválido, nada é salvo).
    // Nomes que já existem (ou repetidos na própria lista) são reaproveitados, não duplicados.
    @Transactional
    public List<Categoria> creatManyCategories(List<String> nomeAll) {
        if (nomeAll == null || nomeAll.isEmpty()) {
            throw new RequisicaoInvalidaException("Envie uma lista com pelo menos um nome, ex.: [\"RPG\", \"Ação\"].");
        }
        for (int i = 0; i < nomeAll.size(); i++) {
            String nome = nomeAll.get(i);
            if (nome == null || nome.isBlank()) {
                throw new RequisicaoInvalidaException("O nome na posição " + i + " da lista está vazio. Nenhuma categoria foi criada.");
            }
            if (nome.strip().length() > 100) {
                throw new RequisicaoInvalidaException("O nome na posição " + i + " passa de 100 caracteres. Nenhuma categoria foi criada.");
            }
        }

        // LinkedHashMap: mantém a ordem da lista e, pela chave (id), não repete a mesma categoria
        Map<Long, Categoria> categorias = new LinkedHashMap<>();
        for (String nomeCategoria : nomeAll) {
            Categoria categoria = buscarOuCriarPorNome(nomeCategoria);
            categorias.put(categoria.getId(), categoria);
        }
        return new ArrayList<>(categorias.values());
    }
}
