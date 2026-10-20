package com.gamelibrary.gamelibrarymanager.dto;

import com.gamelibrary.gamelibrarymanager.model.StatusJogo;

// Filtros da busca GET /jogos. Todos são opcionais (null = não filtra por esse campo)
// e podem ser combinados: ?genero=RPG&status=JOGANDO traz só os RPGs que você está jogando.
// O Spring preenche este record sozinho a partir dos parâmetros da URL (?nome=...&genero=...).
public record FiltroJogosDTO(
        String nome,            // parte do nome, sem diferenciar maiúsculas ("ring" acha "Elden Ring")
        String genero,          // gênero exato, sem diferenciar maiúsculas ("rpg" acha "RPG")
        String plataforma,      // plataforma exata, sem diferenciar maiúsculas
        StatusJogo status,      // JOGANDO, FINALIZADO, ABANDONADO ou PLANEJADO
        Integer anoLancamento,  // ano exato
        String categoria        // nome da categoria, sem diferenciar maiúsculas
) {
}
