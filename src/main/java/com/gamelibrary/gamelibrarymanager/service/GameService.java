package com.gamelibrary.gamelibrarymanager.service;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamConquistaDTO;
import com.gamelibrary.gamelibrarymanager.exception.JogoNaoDeletadoException;
import com.gamelibrary.gamelibrarymanager.exception.JogoNaoEncontradoException;
import com.gamelibrary.gamelibrarymanager.model.Conquista;
import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.model.Platina;
import com.gamelibrary.gamelibrarymanager.repository.ConquistaRepository;
import com.gamelibrary.gamelibrarymanager.repository.JogoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GameService{
    private final JogoRepository jogoRepository;
    private final ConquistaRepository conquistaRepository;
    private final SteamService steamService;


    @Autowired
    public GameService(JogoRepository jogoRepository, ConquistaRepository conquistaRepository, SteamService steamService) {
        this.jogoRepository = jogoRepository;
        this.conquistaRepository = conquistaRepository;
        this.steamService = steamService;
    }

    public List<Jogo> listarTodos(){
        return jogoRepository.findAll();
    }

    public Jogo criarJogo(Jogo jogo) {
        return jogoRepository.save(jogo);
    }

    public Jogo buscarPorId(Long id){
        return  jogoRepository.findById(id).orElseThrow(() -> new JogoNaoEncontradoException(id));


    }

    public Jogo atualizarJogo(Long id,Jogo jogoAtualizado){
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


    public boolean deleteJogo(Long id){
        if(!(jogoRepository.existsById(id))){
           throw new JogoNaoDeletadoException(id);
        }
        jogoRepository.deleteById(id);
        return true;
    }

    public boolean deleteAll(){
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
            Conquista conquista = existentes.get(dto.getApiname());
            if (conquista == null) {
                conquista = new Conquista(jogo, dto.getApiname());
            }
            boolean concluida = dto.getAchieved() == 1;
            conquista.setNome(dto.getName());
            conquista.setDescricao(dto.getDescription());
            conquista.setConcluida(concluida);
            conquista.setDataDesbloqueio(dto.getUnlocktime() > 0 ? Instant.ofEpochSecond(dto.getUnlocktime()) : null);
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

    public List<Jogo>buscarGenero(String genero){
        return jogoRepository.findByGeneroJogo(genero);
    }

    public List<Jogo>buscarJogoPorPlataforma(String plataforma){
        return jogoRepository.findByPlataformaJogo(plataforma);
    }

    public List<Jogo>buscarJogoPorNome(String nome){
        return jogoRepository.findByNomeJogoContainingIgnoreCase(nome);

    }
    public List<Jogo>buscarJogoPorAnoLancamento(int anoLancamento){
        return jogoRepository.findByAnoLancamentoJogo(anoLancamento);
    }
    public List<Jogo>buscarJogoPorStatus(GameStatus status){
        return jogoRepository.findByStatusJogo(status);
    }

}
