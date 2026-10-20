package com.gamelibrary.gamelibrarymanager.exception;

public class CategoriaDuplicadaException extends RuntimeException {
    public CategoriaDuplicadaException(String nome, Long idExistente) {
        super("Já existe a categoria '" + nome + "' (id " + idExistente + "). Use essa em vez de criar outra.");
    }
}
