package com.gamelibrary.gamelibrarymanager.exception;

// Cadastro com um e-mail que já existe. Vira 409 no GlobalExceptionHandler.
// A mensagem não repete o e-mail recebido (não devolvemos dados da requisição em mensagens de erro).
public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException() {
        super("Este e-mail já está cadastrado.");
    }
}
