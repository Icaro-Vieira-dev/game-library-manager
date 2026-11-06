package com.gamelibrary.gamelibrarymanager.service;
import com.gamelibrary.gamelibrarymanager.dto.FiltroJogosDTO;
import com.gamelibrary.gamelibrarymanager.dto.JogoPatchDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamConquistaDTO;
import com.gamelibrary.gamelibrarymanager.exception.JogoNaoEncontradoException;
import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.model.Conquista;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.model.Platina;
import com.gamelibrary.gamelibrarymanager.model.Usuario;
import com.gamelibrary.gamelibrarymanager.repository.ConquistaRepository;
import com.gamelibrary.gamelibrarymanager.repository.JogoEspecificacoes;
import com.gamelibrary.gamelibrarymanager.repository.JogoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Todo método recebe o USUÁRIO LOGADO (vindo do token, pelo @UsuarioLogado do controller) e só
// enxerga os jogos dele. Jogo de outro usuário responde o mesmo 404 de "não existe".
@Service
public class JogoService{
    private final JogoRepository jogoRepository;
    private final ConquistaRepository conquistaRepository;
    private final SteamService steamService;
    private final CategoriaService categoriaService;


    @Autowired
    public JogoService(JogoRepository jogoRepository, ConquistaRepository conquistaRepository,
                       SteamService steamService, CategoriaService categoriaService) {
        this.jogoRepository = jogoRepository;
        this.conquistaRepository = conquistaRepository;
        this.steamService = steamService;
        this.categoriaService = categoriaService;
    }

    // busca com filtros opcionais + paginação, sempre só nos jogos do usuário
    public Page<Jogo> buscar(FiltroJogosDTO filtro, Pageable paginacao, Usuario usuario){
        return jogoRepository.findAll(JogoEspecificacoes.comFiltros(filtro, usuario), paginacao);
    }

    // o jogo nasce com o dono logado
    public Jogo criar(Jogo jogo, Usuario usuario) {
        jogo.setUsuario(usuario);
        return jogoRepository.save(jogo);
    }

    public Jogo buscarPorId(Long id, Usuario usuario){
        return jogoDoUsuario(id, usuario);
    }

    public Jogo atualizar(Long id, Jogo jogoAtualizado, Usuario usuario){
        Jogo jogo = jogoDoUsuario(id, usuario);

        jogo.setNomeJogo(jogoAtualizado.getNomeJogo());
        jogo.setGeneroJogo(jogoAtualizado.getGeneroJogo());
        jogo.setPlataformaJogo(jogoAtualizado.getPlataformaJogo());
        jogo.setAnoLancamentoJogo(jogoAtualizado.getAnoLancamentoJogo());
        jogo.setHorasJogadas(jogoAtualizado.getHorasJogadas());
        jogo.setStatusJogo(jogoAtualizado.getStatusJogo());
        jogo.setNotaJogo(jogoAtualizado.getNotaJogo());
        jogo.setDesenvolvedoraJogo(jogoAtualizado.getDesenvolvedoraJogo());

        return jogoRepository.save(jogo);
    }

    // PATCH: só troca os campos que vieram preenchidos; os que vieram null ficam como estão
    public Jogo atualizarParcialmente(Long id, JogoPatchDTO mudancas, Usuario usuario){
        Jogo jogo = jogoDoUsuario(id, usuario);

        if (mudancas.nomeJogo() != null) jogo.setNomeJogo(mudancas.nomeJogo().strip());
        if (mudancas.generoJogo() != null) jogo.setGeneroJogo(mudancas.generoJogo().strip());
        if (mudancas.plataformaJogo() != null) jogo.setPlataformaJogo(mudancas.plataformaJogo().strip());
        if (mudancas.anoLancamentoJogo() != null) jogo.setAnoLancamentoJogo(mudancas.anoLancamentoJogo());
        if (mudancas.horasJogadas() != null) jogo.setHorasJogadas(mudancas.horasJogadas());
        if (mudancas.statusJogo() != null) jogo.setStatusJogo(mudancas.statusJogo());
        if (mudancas.notaJogo() != null) jogo.setNotaJogo(mudancas.notaJogo());
        if (mudancas.desenvolvedoraJogo() != null) jogo.setDesenvolvedoraJogo(mudancas.desenvolvedoraJogo().strip());

        return jogoRepository.save(jogo);
    }


    // busca filtrando pelo dono e apaga ESSE jogo (os vínculos com categorias saem junto, e as
    // conquistas o banco apaga sozinho: ON DELETE CASCADE)
    public boolean apagar(Long id, Usuario usuario){
        jogoRepository.delete(jogoDoUsuario(id, usuario));
        return true;
    }

    // apaga TODOS os jogos DESSE usuário; os dos outros usuários não são tocados
    @Transactional
    public boolean apagarTodos(Usuario usuario){
        // o DELETE em lote roda SQL direto e não passa pelo JPA, então os vínculos com categorias
        // (tabela jogo_categorias) precisam sair antes; as conquistas o banco apaga sozinho (ON DELETE CASCADE)
        jogoRepository.apagarVinculosComCategoriasDoUsuario(usuario.getId());
        // um único DELETE no banco, em vez de carregar e apagar jogo por jogo
        jogoRepository.apagarTodosDoUsuario(usuario);
        return true;
    }

    // Define a platina do jogo. De onde vêm os números (obtidas/total):
    //   1. vieram no body                              -> usa os do body (manual)
    //   2. não vieram, meta ativa e jogo é da Steam     -> busca na Steam e atualiza o checklist
    //   3. não vieram, nos outros casos                 -> mantém os números que já estavam salvos
    @Transactional
    public Jogo definirPlatina(Long id, Platina platina, Usuario usuario){
        Jogo jogo = jogoDoUsuario(id, usuario);

        // o PlatinaRequestDTO garante que os dois números vêm juntos, então basta olhar um
        boolean semNumeros = platina.getTotalConquista() == null;
        if (semNumeros) {
            if (Boolean.TRUE.equals(platina.getPlatinaAtiva()) && jogo.getSteamAppId() != null) {
                platina = sincronizarComSteam(jogo, usuario);
            } else if (jogo.getPlatina() != null) {
                platina.setConquistaObtida(jogo.getPlatina().getConquistaObtida());
                platina.setTotalConquista(jogo.getPlatina().getTotalConquista());
            }
        }

        jogo.setPlatina(platina);
        return jogoRepository.save(jogo);
    }

    // Busca as conquistas na Steam, grava/atualiza o checklist e devolve a platina com os números.
    // As conquistas são as do DONO do jogo: usa o Steam ID dele (o jogo já foi buscado filtrando pelo dono,
    // então o dono é o usuário logado).
    private Platina sincronizarComSteam(Jogo jogo, Usuario dono) {
        List<SteamConquistaDTO> daSteam = steamService.buscarConquistas(jogo.getSteamAppId(), dono.getSteamId());

        // as que já estão no banco, indexadas pelo apiName, para atualizar em vez de duplicar
        Map<String, Conquista> existentes = new HashMap<>();
        for (Conquista conquista : conquistaRepository.findByJogo_IdJogoAndJogo_UsuarioOrderByIdAsc(jogo.getIdJogo(), dono)) {
            existentes.put(conquista.getApiName(), conquista);
        }

        List<Conquista> paraSalvar = new ArrayList<>();
        int obtidas = 0;
        for (SteamConquistaDTO dto : daSteam) {
            Conquista conquista = existentes.get(dto.apiName());
            if (conquista == null) {
                conquista = new Conquista(jogo, dto.apiName());
            }
            boolean concluida = dto.desbloqueada() == 1;
            conquista.setNome(dto.nome());
            conquista.setDescricao(dto.descricao());
            conquista.setConcluida(concluida);
            conquista.setDataDesbloqueio(dto.momentoDesbloqueio() > 0 ? Instant.ofEpochSecond(dto.momentoDesbloqueio()) : null);
            paraSalvar.add(conquista);
            if (concluida) {
                obtidas++;
            }
        }
        conquistaRepository.saveAll(paraSalvar);

        return new Platina(true, obtidas, daSteam.size());
    }

    // volta o jogo para "sem platina": colunas da platina ficam null e o checklist é apagado
    @Transactional
    public Jogo removerPlatina(Long id, Usuario usuario){
        Jogo jogo = jogoDoUsuario(id, usuario);
        conquistaRepository.apagarDoJogo(id, usuario);
        jogo.setPlatina(null);
        return jogoRepository.save(jogo);
    }

    // liga o jogo a uma categoria; se já estava ligado, não faz nada (o Set não duplica).
    // O jogo E a categoria precisam ser do usuário logado (senão, 404): é essa regra que garante que um
    // vínculo nunca junta coisas de donos diferentes.
    @Transactional
    public Jogo vincularCategoria(Long id, Long categoriaId, Usuario usuario){
        Jogo jogo = jogoDoUsuario(id, usuario);
        Categoria categoria = categoriaService.buscarPorId(categoriaId, usuario);
        jogo.adicionarCategoria(categoria);
        return jogoRepository.save(jogo);
    }

    // desfaz o vínculo; o jogo e a categoria continuam existindo
    @Transactional
    public Jogo desvincularCategoria(Long id, Long categoriaId, Usuario usuario){
        Jogo jogo = jogoDoUsuario(id, usuario);
        Categoria categoria = categoriaService.buscarPorId(categoriaId, usuario);
        jogo.removerCategoria(categoria);
        return jogoRepository.save(jogo);
    }

    // checklist de conquistas; concluida = null traz todas, true só as feitas, false só as que faltam
    public List<Conquista> listarConquistas(Long id, Boolean concluida, Usuario usuario){
        if (!jogoRepository.existsByIdJogoAndUsuario(id, usuario)) {
            throw new JogoNaoEncontradoException(id);
        }
        if (concluida == null) {
            return conquistaRepository.findByJogo_IdJogoAndJogo_UsuarioOrderByIdAsc(id, usuario);
        }
        return conquistaRepository.findByJogo_IdJogoAndJogo_UsuarioAndConcluidaOrderByIdAsc(id, usuario, concluida);
    }

    // O jogo, se existir E for desse usuário. O dono é filtrado NA CONSULTA (não buscamos o jogo para
    // conferir o dono depois). Jogo de outro usuário = mesmo 404 de "não existe": ninguém descobre
    // quais ids existem.
    private Jogo jogoDoUsuario(Long id, Usuario usuario) {
        return jogoRepository.findByIdJogoAndUsuario(id, usuario).orElseThrow(() -> new JogoNaoEncontradoException(id));
    }
}
