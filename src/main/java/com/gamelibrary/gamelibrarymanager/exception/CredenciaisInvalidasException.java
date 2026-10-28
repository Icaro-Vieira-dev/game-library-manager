package com.gamelibrary.gamelibrarymanager.exception;

// Login recusado. Vira 401 no GlobalExceptionHandler.
// É a MESMA exceção, com a MESMA mensagem, para "e-mail não cadastrado" e para "senha errada":
// se as respostas fossem diferentes, daria para descobrir quais e-mails têm conta.
public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos.");
    }
}
