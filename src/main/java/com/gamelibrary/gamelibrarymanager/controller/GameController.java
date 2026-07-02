package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.dto.JogoRequestDTO;
import com.gamelibrary.gamelibrarymanager.dto.JogoResponseDTO;
import com.gamelibrary.gamelibrarymanager.exception.JogoNaoEncontradoException;
import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.model.Platina;
import com.gamelibrary.gamelibrarymanager.service.GameService;
import jakarta.validation.Valid;
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
    public JogoResponseDTO criarJogos(@Valid @RequestBody JogoRequestDTO dto) {
        Jogo jogo = dto.toJogo();
        Jogo jogosalvo = gameService.criarJogo(jogo);
        return JogoResponseDTO.fromJogo(jogosalvo);
    }

    @GetMapping("/{id}")
    public JogoResponseDTO buscaID(@Valid @PathVariable Long id) {
       Jogo jogo = gameService.buscarPorId(id);
       return JogoResponseDTO.fromJogo(jogo);
    }

    @PutMapping("/{id}")
    public JogoResponseDTO atualizarJogo(@PathVariable Long id,@Valid @RequestBody JogoRequestDTO jogo) {
        Jogo request = jogo.toJogo();
        Jogo jogoatualizado = gameService.atualizarJogo(id, request);
        return JogoResponseDTO.fromJogo(jogoatualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirJogo(@PathVariable Long id) {
        gameService.deleteJogo(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/deleteAllJogos")
    public ResponseEntity<Void> deleteAllJogos() {
        gameService.deleteAll();
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/platina")
    public ResponseEntity<JogoResponseDTO> ativarPlatina(@PathVariable Long id, @RequestBody Platina platina) {
        Jogo jogo = gameService.ativarPlatina(id, platina);
        return ResponseEntity.ok(JogoResponseDTO.fromJogo(jogo));

    }

    @GetMapping("buscar/genero")
    public List<JogoResponseDTO>buscarPorGenero(@Valid @RequestParam String genero) {
        List<Jogo> generos = gameService.buscarGenero(genero);
        List<JogoResponseDTO> listaGeneros = new ArrayList<>();

        for (Jogo jogo : generos) {
            listaGeneros.add(JogoResponseDTO.fromJogo(jogo));
        }
        return listaGeneros;
    }

    @GetMapping("buscar/plataforma")
    public List<JogoResponseDTO>buscarPorPLataforma(@RequestParam String plataforma) {
        List<Jogo> plataformas = gameService.buscarJogoPorPlataforma(plataforma);
        List<JogoResponseDTO> listaPlataformas = new ArrayList<>();
        for (Jogo jogo : plataformas) {
            listaPlataformas.add(JogoResponseDTO.fromJogo(jogo));
        }
        return listaPlataformas;
    }

    @GetMapping("buscar/nome")
    public List<JogoResponseDTO> buscarPorNome(@RequestParam String nome) {
        List<Jogo> nomes = gameService.buscarJogoPorNome(nome);
        List<JogoResponseDTO> listaNomes = new ArrayList<>();
        for (Jogo jogo : nomes) {
            listaNomes.add(JogoResponseDTO.fromJogo(jogo));
        }
        return listaNomes;
    }
    @GetMapping("buscar/anoLancamento")
    public List<JogoResponseDTO>buscarPorAnoLancamento(@RequestParam int anoLancamento) {
        List<Jogo> lancaento = gameService.buscarJogoPorAnoLancamento(anoLancamento);
        List<JogoResponseDTO> listaLancamentos = new ArrayList<>();
        for (Jogo jogo : lancaento) {
            listaLancamentos.add(JogoResponseDTO.fromJogo(jogo));
        }
        return listaLancamentos;

    }

    @GetMapping("buscar/status")
    public List<JogoResponseDTO>buscarPorStatus(@RequestParam GameStatus status) {
        List<Jogo> jogoStatus = gameService.buscarJogoPorStatus(status);
        List<JogoResponseDTO> listaJogos = new ArrayList<>();
        for (Jogo jogo : jogoStatus) {
            listaJogos.add(JogoResponseDTO.fromJogo(jogo));
        }
        return listaJogos;
    }
}
