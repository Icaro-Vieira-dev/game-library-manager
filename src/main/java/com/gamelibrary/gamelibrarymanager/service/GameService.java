package com.gamelibrary.gamelibrarymanager.service;
import com.gamelibrary.gamelibrarymanager.exception.JogoNaoDeletadoException;
import com.gamelibrary.gamelibrarymanager.exception.JogoNaoEncontradoException;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.model.Platina;
import com.gamelibrary.gamelibrarymanager.repository.JogoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GameService{
    private final JogoRepository jogoRepository;


    @Autowired
    public GameService(JogoRepository jogoRepository) {
        this.jogoRepository = jogoRepository;
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

    public Jogo ativarPlatina(Long id, Platina platina){
        Jogo jogo = jogoRepository.findById(id).orElseThrow(() -> new JogoNaoEncontradoException(id));
        platina.setPlatinaAtiva(true);
        jogo.setPlatina(platina);
        return jogoRepository.save(jogo);
    }
}
