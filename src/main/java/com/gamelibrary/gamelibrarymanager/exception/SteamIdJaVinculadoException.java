package com.gamelibrary.gamelibrarymanager.exception;

// Cadastro com um Steam ID que já está em outra conta. Vira 409 no GlobalExceptionHandler.
// A mensagem não repete o Steam ID recebido nem diz QUAL conta o usa.
public class SteamIdJaVinculadoException extends RuntimeException {
    public SteamIdJaVinculadoException() {
        super("Este Steam ID já está ligado a outra conta.");
    }
}
