package com.gamelibrary.gamelibrarymanager.service;
import com.gamelibrary.gamelibrarymanager.exception.CategoriaDuplicadaException;
import com.gamelibrary.gamelibrarymanager.exception.CategoriaEmUsoException;
import com.gamelibrary.gamelibrarymanager.exception.CategoriaNaoEncontradaException;
import com.gamelibrary.gamelibrarymanager.exception.RequisicaoInvalidaException;
import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.model.Usuario;
import com.gamelibrary.gamelibrarymanager.repository.CategoriaRepository;
import com.gamelibrary.gamelibrarymanager.repository.JogoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Categorias são POR USUÁRIO: todo método recebe o usuário logado e só enxerga as categorias dele.
@Service
public class CategoriaService{
    private final CategoriaRepository categoriaRepository;
    private final JogoRepository jogoRepository;

    @Autowired
    public CategoriaService(CategoriaRepository categoriaRepository, JogoRepository jogoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.jogoRepository = jogoRepository;
    }

    // não deixa o mesmo usuário criar duas categorias com o mesmo nome ("RPG" e "rpg" contam como iguais).
    // A categoria já chega com o dono (CategoriaRequestDTO.toCategoria).
    public Categoria criar(Categoria categoria) {
        categoriaRepository.findByUsuarioAndNomeNormalizado(categoria.getUsuario(), categoria.getNomeNormalizado())
                .ifPresent(existente -> {
                    throw new CategoriaDuplicadaException(existente.getNome(), existente.getId());
                });
        return salvarNova(categoria);
    }

    public List<Categoria> listarTodas(Usuario usuario) {
        return categoriaRepository.findByUsuario(usuario);
    }

    // de outro usuário = mesmo 404 de "não existe" (ninguém descobre quais ids existem)
    public Categoria buscarPorId(Long id, Usuario usuario) {
        return categoriaRepository.findByIdAndUsuario(id, usuario).orElseThrow(() -> new CategoriaNaoEncontradaException(id));
    }

    public List<Categoria> buscarPorNome(String nome, Usuario usuario) {
        return categoriaRepository.findByUsuarioAndNomeNormalizadoContaining(usuario, Categoria.normalizarNome(nome));
    }

    // só apaga categoria sem jogos; com jogos, responde 409 dizendo quantos
    public boolean apagar(Long id, Usuario usuario) {
        Categoria categoria = buscarPorId(id, usuario);
        long jogosNaCategoria = jogoRepository.countByUsuarioAndCategorias_Id(usuario, id);
        if (jogosNaCategoria > 0) {
            throw new CategoriaEmUsoException("A categoria " + id + " tem " + jogosNaCategoria + " jogo(s). "
                    + "Tire os jogos dela antes (DELETE /jogos/{id}/categorias/" + id + ").");
        }
        categoriaRepository.delete(categoria);
        return true;
    }

    // mesma regra: se qualquer categoria DO USUÁRIO tiver jogos, não apaga nenhuma.
    // Só as categorias dele são apagadas; as dos outros usuários nem entram na conta.
    @Transactional
    public boolean apagarTodas(Usuario usuario) {
        long vinculos = jogoRepository.contarVinculosComCategorias(usuario);
        if (vinculos > 0) {
            throw new CategoriaEmUsoException("Existem " + vinculos + " vínculo(s) entre jogos e categorias. "
                    + "Tire os jogos das categorias antes de apagar todas.");
        }
        categoriaRepository.deleteAll(categoriaRepository.findByUsuario(usuario));
        return true;
    }

    // usado pela Steam e pelo lote: reaproveita a categoria DO USUÁRIO se já existir (sem diferenciar
    // maiúsculas), senão cria uma para ele
    public Categoria buscarOuCriarPorNome(String nome, Usuario usuario) {
        return categoriaRepository.findByUsuarioAndNomeNormalizado(usuario, Categoria.normalizarNome(nome))
                .orElseGet(() -> salvarNova(new Categoria(nome.strip(), usuario)));
    }

    // Cria várias de uma vez. Primeiro valida TODOS os nomes (se um for inválido, nada é salvo).
    // Nomes que já existem (ou repetidos na própria lista) são reaproveitados, não duplicados.
    @Transactional
    public List<Categoria> criarVarias(List<String> nomeAll, Usuario usuario) {
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
            Categoria categoria = buscarOuCriarPorNome(nomeCategoria, usuario);
            categorias.put(categoria.getId(), categoria);
        }
        return new ArrayList<>(categorias.values());
    }

    // Salva uma categoria nova. Dois pedidos ao mesmo tempo com o mesmo nome passam os dois pela
    // conferência no código, e o UNIQUE (usuario_id, nome_normalizado) do banco recusa o segundo: aqui ele
    // vira 409, e não 500. É o único UNIQUE da tabela (nome e dono nunca chegam vazios), por isso a
    // violação só pode ser o nome repetido. Não dá para buscar o id da que ganhou: dentro de uma transação,
    // depois do erro, o banco não aceita mais consultas (e a transação inteira é desfeita).
    private Categoria salvarNova(Categoria categoria) {
        try {
            return categoriaRepository.save(categoria);
        } catch (DataIntegrityViolationException e) {
            throw new CategoriaDuplicadaException(categoria.getNome());
        }
    }
}
