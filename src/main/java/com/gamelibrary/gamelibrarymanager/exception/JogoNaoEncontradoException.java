package com.gamelibrary.gamelibrarymanager.exception;

public class JogoNaoEncontradoException extends RuntimeException {
    public JogoNaoEncontradoException(Long id) {
        super("Jogo do id: " + id + " Não encontrado");
    }
}
