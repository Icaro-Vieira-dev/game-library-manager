package com.gamelibrary.gamelibrarymanager.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Teste unitário puro do extrairAno: a loja da Steam manda a data em texto, e o formato muda com o idioma.
class SteamServiceAnoTest {

    @Test
    void formatoBrasileiro() {
        assertEquals(2022, SteamService.extrairAno("24/fev./2022"));
    }

    @Test
    void formatoIngles() {
        assertEquals(2022, SteamService.extrairAno("Feb 24, 2022"));
    }

    @Test
    void soOAno() {
        assertEquals(2018, SteamService.extrairAno("2018"));
    }

    @Test
    void semAno_emBreve() {
        assertEquals(0, SteamService.extrairAno("Em breve"));
    }

    @Test
    void dataNula() {
        assertEquals(0, SteamService.extrairAno(null));
    }
}
