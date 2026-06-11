package com.gamelibrary.gamelibrarymanager.exception;

import com.gamelibrary.gamelibrarymanager.dto.JogoResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

}
