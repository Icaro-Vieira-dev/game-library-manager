package com.gamelibrary.gamelibrarymanager.service;

import com.gamelibrary.gamelibrarymanager.dto.steam.SteamJogoDTO;
import com.gamelibrary.gamelibrarymanager.model.StatusJogo;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Teste unitário da mescla da importação (item 9): o que é novo, o que só tem as horas atualizadas.
// Não usa banco nem a Steam: monta os dados à mão e confere o resultado.
class SteamServiceMesclaTest {

    private static final Instant AGORA = Instant.parse("2026-10-20T12:00:00Z");

    private SteamJogoDTO daSteam(int appId, String nome, int minutos) {
        // nunca jogado recentemente (0 nas 2 semanas e sem data): o status sai pelas horas
        return new SteamJogoDTO(appId, nome, minutos, 0, 0L);
    }

    private Jogo existente(long appId, double horas, StatusJogo status) {
        Jogo jogo = new Jogo();
        jogo.setSteamAppId(appId);
        jogo.setHorasJogadas(horas);
        jogo.setStatusJogo(status);
        jogo.setNotaJogo(9);
        return jogo;
    }

    @Test
    void jogoNovoEhCadastrado() {
        var resultado = SteamService.mesclarBiblioteca(List.of(daSteam(10, "Hades", 120)), new HashMap<>(), AGORA);

        assertThat(resultado.novos()).hasSize(1);
        assertThat(resultado.novos().get(0).getNomeJogo()).isEqualTo("Hades");
        assertThat(resultado.novos().get(0).getHorasJogadas()).isEqualTo(2.0);
        assertThat(resultado.atualizados()).isEmpty();
    }

    @Test
    void jogoExistente_atualizaSoAsHoras() {
        Jogo elden = existente(20, 50, StatusJogo.FINALIZADO);
        Map<Long, Jogo> noBanco = new HashMap<>(Map.of(20L, elden));

        var resultado = SteamService.mesclarBiblioteca(List.of(daSteam(20, "ELDEN RING", 6000)), noBanco, AGORA);

        assertThat(resultado.novos()).isEmpty();
        assertThat(resultado.atualizados()).containsExactly(elden);
        assertThat(elden.getHorasJogadas()).isEqualTo(100.0);
        // o que você ajustou à mão não muda
        assertThat(elden.getStatusJogo()).isEqualTo(StatusJogo.FINALIZADO);
        assertThat(elden.getNotaJogo()).isEqualTo(9);
    }

    @Test
    void jogoExistenteSemMudancaDeHoras_naoEntraNosAtualizados() {
        Map<Long, Jogo> noBanco = new HashMap<>(Map.of(30L, existente(30, 2.0, StatusJogo.ABANDONADO)));

        var resultado = SteamService.mesclarBiblioteca(List.of(daSteam(30, "Portal", 120)), noBanco, AGORA);

        assertThat(resultado.novos()).isEmpty();
        assertThat(resultado.atualizados()).isEmpty();
    }

    @Test
    void appidRepetidoNaRespostaDaSteam_naoCriaDuasVezes() {
        var resultado = SteamService.mesclarBiblioteca(
                List.of(daSteam(40, "Dispatch", 60), daSteam(40, "Dispatch", 60)), new HashMap<>(), AGORA);

        assertThat(resultado.novos()).hasSize(1);
    }
}
