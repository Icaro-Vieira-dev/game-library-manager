package com.gamelibrary.gamelibrarymanager.exception;

public class CategoriaNaoEncontrada extends RuntimeException {
    public CategoriaNaoEncontrada(long id) {
        super("Categoria ID: " + id + " Não encontrada verifique e faça o reenvio.");
    }
}
