package com.gamelibrary.gamelibrarymanager.service;
import com.gamelibrary.gamelibrarymanager.dto.FiltroJogosDTO;
import com.gamelibrary.gamelibrarymanager.dto.JogoPatchDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamConquistaDTO;
import com.gamelibrary.gamelibrarymanager.exception.JogoNaoEncontradoException;
import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.model.Conquista;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.model.Platina;
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

    // busca com filtros opcionais + paginação (substitui o listarTodos e os 5 buscar*)
    public Page<Jogo> buscar(FiltroJogosDTO filtro, Pageable paginacao){
        return jogoRepository.findAll(JogoEspecificacoes.comFiltros(filtro), paginacao);
    }

    public Jogo criar(Jogo jogo) {
        return jogoRepository.save(jogo);
    }

    public Jogo buscarPorId(Long id){
        return  jogoRepository.findById(id).orElseThrow(() -> new JogoNaoEncontradoException(id));


    }

    public Jogo atualizar(Long id, Jogo jogoAtualizado){
        Jogo jogo = jogoRepository.findById(id).orElseThrow(() -> new JogoNaoEncontradoException(id));

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
    public Jogo atualizarParcialmente(Long id, JogoPatchDTO mudancas){
        Jogo jogo = jogoRepository.findById(id).orElseThrow(() -> new JogoNaoEncontradoException(id));

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


    public boolean apagar(Long id){
        if(!(jogoRepository.existsById(id))){
           throw new JogoNaoEncontradoException(id);
        }
        jogoRepository.deleteById(id);
        return true;
    }

    @Transactional
    public boolean apagarTodos(){
        // o deleteAllInBatch roda SQL direto e não passa pelo JPA, então os vínculos com categorias
        // (tabela jogo_categorias) precisam sair antes; as conquistas o banco apaga sozinho (ON DELETE CASCADE)
        jogoRepository.apagarTodosVinculosComCategorias();
        // um único DELETE no banco, em vez de carregar e apagar jogo por jogo
        jogoRepository.deleteAllInBatch();
        return true;
    }

    // Define a platina do jogo. De onde vêm os números (obtidas/total):
    //   1. vieram no body                              -> usa os do body (manual)
    //   2. não vieram, meta ativa e jogo é da Steam     -> busca na Steam e atualiza o checklist
    //   3. não vieram, nos outros casos                 -> mantém os números que já estavam salvos
    @Transactional
    public Jogo definirPlatina(Long id, Platina platina){
        Jogo jogo = jogoRepository.findById(id).orElseThrow(() -> new JogoNaoEncontradoException(id));

        // o PlatinaRequestDTO garante que os dois números vêm juntos, então basta olhar um
        boolean semNumeros = platina.getTotalConquista() == null;
        if (semNumeros) {
            if (Boolean.TRUE.equals(platina.getPlatinaAtiva()) && jogo.getSteamAppId() != null) {
                platina = sincronizarComSteam(jogo);
            } else if (jogo.getPlatina() != null) {
                platina.setConquistaObtida(jogo.getPlatina().getConquistaObtida());
                platina.setTotalConquista(jogo.getPlatina().getTotalConquista());
            }
        }

        jogo.setPlatina(platina);
        return jogoRepository.save(jogo);
    }

    // Busca as conquistas na Steam, grava/atualiza o checklist e devolve a platina com os números.
    private Platina sincronizarComSteam(Jogo jogo) {
        List<SteamConquistaDTO> daSteam = steamService.buscarConquistas(jogo.getSteamAppId());

        // as que já estão no banco, indexadas pelo apiName, para atualizar em vez de duplicar
        Map<String, Conquista> existentes = new HashMap<>();
        for (Conquista conquista : conquistaRepository.findByJogo_IdJogoOrderByIdAsc(jogo.getIdJogo())) {
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
    public Jogo removerPlatina(Long id){
        Jogo jogo = jogoRepository.findById(id).orElseThrow(() -> new JogoNaoEncontradoException(id));
        conquistaRepository.deleteByJogoId(id);
        jogo.setPlatina(null);
        return jogoRepository.save(jogo);
    }

    // liga o jogo a uma categoria; se já estava ligado, não faz nada (o Set não duplica)
    @Transactional
    public Jogo vincularCategoria(Long id, Long categoriaId){
        Jogo jogo = jogoRepository.findById(id).orElseThrow(() -> new JogoNaoEncontradoException(id));
        Categoria categoria = categoriaService.buscarPorId(categoriaId);
        jogo.adicionarCategoria(categoria);
        return jogoRepository.save(jogo);
    }

    // desfaz o vínculo; o jogo e a categoria continuam existindo
    @Transactional
    public Jogo desvincularCategoria(Long id, Long categoriaId){
        Jogo jogo = jogoRepository.findById(id).orElseThrow(() -> new JogoNaoEncontradoException(id));
        Categoria categoria = categoriaService.buscarPorId(categoriaId);
        jogo.removerCategoria(categoria);
        return jogoRepository.save(jogo);
    }

    // checklist de conquistas; concluida = null traz todas, true só as feitas, false só as que faltam
    public List<Conquista> listarConquistas(Long id, Boolean concluida){
        if (!jogoRepository.existsById(id)) {
            throw new JogoNaoEncontradoException(id);
        }
        if (concluida == null) {
            return conquistaRepository.findByJogo_IdJogoOrderByIdAsc(id);
        }
        return conquistaRepository.findByJogo_IdJogoAndConcluidaOrderByIdAsc(id, concluida);
    }

}
