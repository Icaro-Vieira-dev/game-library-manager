package com.gamelibrary.gamelibrarymanager.service;

import com.gamelibrary.gamelibrarymanager.dto.steam.SteamGameDTO;
import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Teste unitário puro: não sobe o Spring, não usa banco nem a Steam.
class SteamServiceStatusTest {

    // data fixa, para o resultado não depender do dia em que o teste roda
    private static final Instant AGORA = Instant.parse("2026-10-19T12:00:00Z");

    private SteamGameDTO jogo(int minutosTotal, int minutos2Semanas, Instant ultimaVez) {
        SteamGameDTO dto = new SteamGameDTO();
        dto.setPlaytime_forever(minutosTotal);
        dto.setPlaytime_2weeks(minutos2Semanas);
        dto.setRtime_last_played(ultimaVez == null ? 0 : ultimaVez.getEpochSecond());
        return dto;
    }

    @Test
    void nuncaJogou_planejado() {
        assertEquals(GameStatus.PLANEJADO, SteamService.definirStatus(jogo(0, 0, null), AGORA));
    }

    @Test
    void jogouNasUltimas2Semanas_jogando() {
        assertEquals(GameStatus.JOGANDO, SteamService.definirStatus(jogo(600, 90, AGORA.minus(Duration.ofDays(3))), AGORA));
    }

    @Test
    void semMinutosRecentesMasAbriuHa5Dias_jogando() {
        assertEquals(GameStatus.JOGANDO, SteamService.definirStatus(jogo(600, 0, AGORA.minus(Duration.ofDays(5))), AGORA));
    }

    @Test
    void poucasHorasEUltimaVezHa1Ano_abandonado() {
        assertEquals(GameStatus.ABANDONADO, SteamService.definirStatus(jogo(120, 0, AGORA.minus(Duration.ofDays(365))), AGORA));
    }

    @Test
    void poucasHorasSemDataDeUltimaVez_abandonado() {
        assertEquals(GameStatus.ABANDONADO, SteamService.definirStatus(jogo(120, 0, null), AGORA));
    }

    // caso do Resident Evil 4: zerou (25h) e não joga mais
    @Test
    void muitasHorasEUltimaVezHa1Ano_finalizado() {
        assertEquals(GameStatus.FINALIZADO, SteamService.definirStatus(jogo(1500, 0, AGORA.minus(Duration.ofDays(365))), AGORA));
    }

    // limite: exatamente 10h já conta como finalizado; 1 minuto a menos, não
    @Test
    void exatamente10Horas_finalizado() {
        assertEquals(GameStatus.FINALIZADO, SteamService.definirStatus(jogo(600, 0, AGORA.minus(Duration.ofDays(30))), AGORA));
    }

    @Test
    void umMinutoAbaixoDe10Horas_abandonado() {
        assertEquals(GameStatus.ABANDONADO, SteamService.definirStatus(jogo(599, 0, AGORA.minus(Duration.ofDays(30))), AGORA));
    }

    // jogar recentemente vence as horas: 25h e jogou ontem = ainda está jogando
    @Test
    void muitasHorasMasJogouOntem_jogando() {
        assertEquals(GameStatus.JOGANDO, SteamService.definirStatus(jogo(1500, 60, AGORA.minus(Duration.ofDays(1))), AGORA));
    }
}
