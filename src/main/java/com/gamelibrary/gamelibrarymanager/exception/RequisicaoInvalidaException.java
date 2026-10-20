package com.gamelibrary.gamelibrarymanager.exception;

// Erro 400 para validações feitas à mão no service (quando as anotações como @NotBlank não alcançam)
public class RequisicaoInvalidaException extends RuntimeException {
    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
