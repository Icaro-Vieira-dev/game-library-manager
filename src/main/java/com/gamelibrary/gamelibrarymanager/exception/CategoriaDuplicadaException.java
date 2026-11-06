package com.gamelibrary.gamelibrarymanager.exception;

public class CategoriaDuplicadaException extends RuntimeException {
    public CategoriaDuplicadaException(String nome, Long idExistente) {
        super("Já existe a categoria '" + nome + "' (id " + idExistente + "). Use essa em vez de criar outra.");
    }

    // quando o BANCO recusou (dois pedidos ao mesmo tempo): aí não dá para buscar o id da que ganhou
    public CategoriaDuplicadaException(String nome) {
        super("Já existe a categoria '" + nome + "'. Use essa em vez de criar outra.");
    }
}
