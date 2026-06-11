package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.dto.JogoRequestDTO;
import com.gamelibrary.gamelibrarymanager.dto.JogoResponseDTO;
import com.gamelibrary.gamelibrarymanager.exception.JogoNaoEncontradoException;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.model.Platina;
import com.gamelibrary.gamelibrarymanager.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@RestController
@RequestMapping("/jogos")
public class GameController {
    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public List<JogoResponseDTO> listarJogos() {
        List<Jogo> jogos = gameService.listarTodos();
        List<JogoResponseDTO> listaJogos = new ArrayList<>();

        for (Jogo jogo : jogos) {
            JogoResponseDTO responseDTO = JogoResponseDTO.fromJogo(jogo);
            listaJogos.add(responseDTO);

        }
        return listaJogos;
    }

    @PostMapping
    public JogoResponseDTO criarJogos(@RequestBody JogoRequestDTO dto) {
        Jogo jogo = dto.toJogo();
        Jogo jogosalvo = gameService.criarJogo(jogo);
        return JogoResponseDTO.fromJogo(jogosalvo);
    }

    @GetMapping("/{id}")
    public JogoResponseDTO buscaID(@PathVariable Long id) {
       Jogo jogo = gameService.buscarPorId(id);
       return JogoResponseDTO.fromJogo(jogo);
    }

    @PutMapping("/{id}")
    public JogoResponseDTO atualizarJogo(@PathVariable Long id, @RequestBody JogoRequestDTO jogo) {
        Jogo request = jogo.toJogo();
        Jogo jogoatualizado = gameService.atualizarJogo(id, request);
        return JogoResponseDTO.fromJogo(jogoatualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirJogo(@PathVariable Long id) {
        gameService.deleteJogo(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/platina")
    public ResponseEntity<JogoResponseDTO> ativarPlatina(@PathVariable Long id, @RequestBody Platina platina) {
        Jogo jogo = gameService.ativarPlatina(id, platina);
        return ResponseEntity.ok(JogoResponseDTO.fromJogo(jogo));

    }
}
