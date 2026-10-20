package com.gamelibrary.gamelibrarymanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.model.StatusJogo;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.model.Platina;

import java.util.List;

// No JSON, os campos saem na ordem em que estão declarados aqui (num record não precisa do @JsonPropertyOrder)
public record JogoResponseDTO(
        Long idJogo,
        String nomeJogo,
        String generoJogo,
        // só os nomes, em ordem alfabética. Se fosse o objeto Categoria inteiro, cada categoria
        // listaria seus jogos, que listariam suas categorias... (loop infinito no JSON)
        List<String> categorias,
        String plataformaJogo,
        int anoLancamentoJogo,
        double horasJogadas,
        StatusJogo statusJogo,
        double notaJogo,
        String desenvolvedoraJogo,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long steamAppId,

        // banner horizontal (460x215), sempre existe nos jogos que passaram pelo /steam/detalhes
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String imagemBanner,

        // capa vertical (600x900), montada pelo appid. A Steam não garante que exista para todo jogo:
        // no front-end, se não carregar, use a imagemBanner no lugar.
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String imagemCapa,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Platina platina
) {
    // %d = steamAppId. Calculada na hora, não fica salva no banco.
    private static final String URL_CAPA_STEAM = "https://cdn.akamai.steamstatic.com/steam/apps/%d/library_600x900.jpg";

    public static JogoResponseDTO deJogo(Jogo jogo) {
        return new JogoResponseDTO(
                jogo.getIdJogo(),
                jogo.getNomeJogo(),
                jogo.getGeneroJogo(),
                jogo.getCategorias().stream().map(Categoria::getNome).sorted().toList(),
                jogo.getPlataformaJogo(),
                jogo.getAnoLancamentoJogo(),
                jogo.getHorasJogadas(),
                jogo.getStatusJogo(),
                jogo.getNotaJogo(),
                jogo.getDesenvolvedoraJogo(),
                jogo.getSteamAppId(),
                jogo.getImagemBanner(),
                jogo.getSteamAppId() == null ? null : String.format(URL_CAPA_STEAM, jogo.getSteamAppId()),
                jogo.getPlatina());
    }
}
