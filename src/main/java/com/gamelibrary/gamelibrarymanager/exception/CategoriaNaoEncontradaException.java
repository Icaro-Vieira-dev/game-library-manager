package com.gamelibrary.gamelibrarymanager.exception;

public class CategoriaNaoEncontradaException extends RuntimeException {
    public CategoriaNaoEncontradaException(long id) {
        super("Categoria ID: " + id + " Não encontrada verifique e faça o reenvio.");
    }
}
