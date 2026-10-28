package com.gamelibrary.gamelibrarymanager.dto;

import java.time.OffsetDateTime;
import java.util.List;

// A vitrine da loja da Steam (GET /vitrine): lançamentos e jogos que ainda vão sair.
public record VitrineResponseDTO(
        List<JogoVitrineDTO> lancamentos,
        List<JogoVitrineDTO> emBreve,
        // quando a vitrine foi montada (UTC). null = ainda não foi carregada desde que a aplicação subiu
        OffsetDateTime atualizadoEm
) {
}
