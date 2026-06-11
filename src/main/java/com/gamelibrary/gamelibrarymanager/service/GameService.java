package com.gamelibrary.gamelibrarymanager.service;
import com.gamelibrary.gamelibrarymanager.exception.JogoNaoDeletadoException;
import com.gamelibrary.gamelibrarymanager.exception.JogoNaoEncontradoException;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.model.Platina;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GameService{
    private List<Jogo> jogos = new ArrayList<>();
    private Long proximoId = 1L;

    public List<Jogo> listarTodos(){
        return jogos;
    }

    public Jogo criarJogo(Jogo jogo) {
        jogo.setIdJogo(proximoId++);
        jogos.add(jogo);
        return jogo;
    }

    public Jogo buscarPorId(Long id){
        for (Jogo jogo : jogos){
            if (jogo.getIdJogo().equals(id)){
                return jogo;
            }
        }
        throw new JogoNaoEncontradoException(id);

    }

    public Jogo atualizarJogo(Long id,Jogo jogoAtualizado){
        for (Jogo jogo : jogos){
            if (jogo.getIdJogo().equals(id)){
                jogo.setNomeJogo(jogoAtualizado.getNomeJogo());
                jogo.setGeneroJogo(jogoAtualizado.getGeneroJogo());
                jogo.setPlataformaJogo(jogoAtualizado.getPlataformaJogo());
                jogo.setAnoLancamentoJogo(jogoAtualizado.getAnoLancamentoJogo());
                jogo.setHorasJogadas(jogoAtualizado.getHorasJogadas());
                jogo.setStatusJogo(jogoAtualizado.getStatusJogo());
                jogo.setNotaJogo(jogoAtualizado.getNotaJogo());
                jogo.setDesenvolvedoraJogo(jogoAtualizado.getDesenvolvedoraJogo());
                return jogo;
            }
        }
        throw new JogoNaoEncontradoException(id);
    }

    public boolean deleteJogo(Long id){
        for (Jogo jogo : jogos){
            if (jogo.getIdJogo().equals(id)){
                jogos.remove(jogo);
                return true;
            }
        }
        throw new JogoNaoDeletadoException(id);
    }

    public Jogo ativarPlatina(Long id, Platina platina){
        for (Jogo jogo : jogos){
            if (jogo.getIdJogo().equals(id)){
                platina.setPlatinaAtiva(true);
                jogo.setPlatina(platina);
                return jogo;
            }
        }
        throw new JogoNaoEncontradoException(id);
    }
}
