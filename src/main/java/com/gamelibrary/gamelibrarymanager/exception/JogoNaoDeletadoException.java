package com.gamelibrary.gamelibrarymanager.exception;

public class JogoNaoDeletadoException extends RuntimeException {
    public JogoNaoDeletadoException(long id) {
        super("Não existe um jogo com esse id: " + id + " Não foi possivel apagar");
    }
}
