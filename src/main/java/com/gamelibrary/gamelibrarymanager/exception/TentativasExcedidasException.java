package com.gamelibrary.gamelibrarymanager.exception;

// Muitas tentativas de login erradas seguidas (LimiteTentativasLogin). Vira 429 no GlobalExceptionHandler,
// com o cabeçalho Retry-After dizendo em quantos segundos dá para tentar de novo.
public class TentativasExcedidasException extends RuntimeException {
    private final long segundosParaTentarDeNovo;

    public TentativasExcedidasException(long segundosParaTentarDeNovo) {
        super("Muitas tentativas de login. Tente de novo mais tarde.");
        this.segundosParaTentarDeNovo = segundosParaTentarDeNovo;
    }

    public long getSegundosParaTentarDeNovo() {
        return segundosParaTentarDeNovo;
    }
}
