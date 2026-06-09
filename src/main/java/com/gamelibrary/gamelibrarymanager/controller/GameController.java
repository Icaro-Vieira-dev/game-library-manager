package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.model.Platina;
import com.gamelibrary.gamelibrarymanager.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jogos")
public class GameController {
    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public List<Jogo> listarJogos() {
        return gameService.listarTodos();
    }

    @PostMapping
    public Jogo criarJogos(@RequestBody Jogo jogo) {
        return gameService.criarJogo(jogo);
    }

    @GetMapping("/{id}")
    public Jogo buscaID(@PathVariable Long id) {
        return gameService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public Jogo atualizarJogo(@PathVariable Long id, @RequestBody Jogo jogo) {
        return gameService.atualizarJogo(id,  jogo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirJogo(@PathVariable Long id) {
        if(gameService.deleteJogo(id)){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/platina")
    public ResponseEntity<Jogo> put(@PathVariable Long id, @RequestBody Platina platina) {
        Jogo jogo = gameService.ativarPlatina(id, platina);

        if (jogo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(jogo);

    }
}
