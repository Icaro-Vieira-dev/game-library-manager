package com.gamelibrary.gamelibrarymanager.exception;

import com.gamelibrary.gamelibrarymanager.dto.JogoResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JogoNaoEncontradoException.class)
    public ResponseEntity<String> handleJogoNaoEncontrado(JogoNaoEncontradoException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(JogoNaoDeletadoException.class)
    public ResponseEntity<String> handleJogoNaoDeletado(JogoNaoDeletadoException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(CategoriaNaoEncontrada.class)
    public ResponseEntity<String> handleCategoriaNaoEncontrada(CategoriaNaoEncontrada ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(erro -> {
            erros.put(erro.getField(), erro.getDefaultMessage());
        });

        return ResponseEntity.status(400).body(erros);
    }

    // ex.: DELETE /jogos/deleteAll cai em /jogos/{id} e "deleteAll" não é um número
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(400).body("Valor inválido para '" + ex.getName() + "': " + ex.getValue()
                + ". Verifique se a rota está correta.");
    }

    @ExceptionHandler(SteamImportacaoException.class)
    public ResponseEntity<String> handleSteamImportacao(SteamImportacaoException ex) {
        return ResponseEntity.status(422).body(ex.getMessage());
    }

    // 409 Conflict: o pedido é válido, mas conflita com o estado atual (a categoria ainda tem jogos)
    @ExceptionHandler(CategoriaEmUsoException.class)
    public ResponseEntity<String> handleCategoriaEmUso(CategoriaEmUsoException ex) {
        return ResponseEntity.status(409).body(ex.getMessage());
    }

    // 409 também: criar uma categoria que já existe conflita com o que está no banco
    @ExceptionHandler(CategoriaDuplicadaException.class)
    public ResponseEntity<String> handleCategoriaDuplicada(CategoriaDuplicadaException ex) {
        return ResponseEntity.status(409).body(ex.getMessage());
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<String> handleRequisicaoInvalida(RequisicaoInvalidaException ex) {
        return ResponseEntity.status(400).body(ex.getMessage());
    }

}
