package com.gamelibrary.gamelibrarymanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.gamelibrary.gamelibrarymanager.model.Categoria;
import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.model.Platina;

import java.util.List;

@JsonPropertyOrder({"idJogo", "nomeJogo", "generoJogo", "categorias", "plataformaJogo", "anoLancamentoJogo", "horasJogadas", "statusJogo", "notaJogo", "desenvolvedoraJogo"})
public class JogoResponseDTO {
    // %d = steamAppId. Calculada na hora, não fica salva no banco.
    private static final String URL_CAPA_STEAM = "https://cdn.akamai.steamstatic.com/steam/apps/%d/library_600x900.jpg";

    private Long idJogo;
    private String nomeJogo;
    private String generoJogo;
    private String plataformaJogo;
    private int anoLancamentoJogo;
    private double horasJogadas;
    private GameStatus statusJogo;
    private double notaJogo;
    private String desenvolvedoraJogo;
    // só os nomes, em ordem alfabética. Se fosse o objeto Categoria inteiro, cada categoria
    // listaria seus jogos, que listariam suas categorias... (loop infinito no JSON)
    private List<String> categorias;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long steamAppId;

    // banner horizontal (460x215), sempre existe nos jogos que passaram pelo /importSteam/detalhes
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String imagemBanner;

    // capa vertical (600x900), montada pelo appid. A Steam não garante que exista para todo jogo:
    // no front-end, se não carregar, use a imagemBanner no lugar.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String imagemCapa;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Platina platina;



    // Getter

    public Long getIdJogo() {
        return idJogo;
    }

    public String getNomeJogo() {
        return nomeJogo;
    }

    public String getGeneroJogo() {
        return generoJogo;
    }

    public String getPlataformaJogo() {
        return plataformaJogo;
    }

    public int getAnoLancamentoJogo() {
        return anoLancamentoJogo;
    }

    public double getHorasJogadas() {
        return horasJogadas;
    }

    public GameStatus getStatusJogo() {
        return statusJogo;
    }

    public double getNotaJogo() {
        return notaJogo;
    }

    public String getDesenvolvedoraJogo() {
        return desenvolvedoraJogo;
    }

    public List<String> getCategorias() {
        return categorias;
    }

    public Long getSteamAppId() {
        return steamAppId;
    }

    public String getImagemBanner() {
        return imagemBanner;
    }

    public String getImagemCapa() {
        return imagemCapa;
    }

    public Platina getPlatina() {
        return platina;
    }

    //setter

    public void setIdJogo(Long idJogo) {
        this.idJogo = idJogo;
    }

    public void setNomeJogo(String nomeJogo) {
        this.nomeJogo = nomeJogo;
    }

    public void setGeneroJogo(String generoJogo) {
        this.generoJogo = generoJogo;
    }

    public void setPlataformaJogo(String plataformaJogo) {
        this.plataformaJogo = plataformaJogo;
    }

    public void setAnoLancamentoJogo(int anoLancamentoJogo) {
        this.anoLancamentoJogo = anoLancamentoJogo;
    }

    public void setHorasJogadas(double horasJogadas) {
        this.horasJogadas = horasJogadas;
    }

    public void setStatusJogo(GameStatus statusJogo) {
        this.statusJogo = statusJogo;
    }

    public void setNotaJogo(double notaJogo) {
        this.notaJogo = notaJogo;
    }

    public void setDesenvolvedoraJogo(String desenvolvedoraJogo) {
        this.desenvolvedoraJogo = desenvolvedoraJogo;
    }

    public void setCategorias(List<String> categorias) {
        this.categorias = categorias;
    }

    public void setSteamAppId(Long steamAppId) {
        this.steamAppId = steamAppId;
    }

    public void setImagemBanner(String imagemBanner) {
        this.imagemBanner = imagemBanner;
    }

    public void setImagemCapa(String imagemCapa) {
        this.imagemCapa = imagemCapa;
    }

    public void setPlatina(Platina platina) {
        this.platina = platina;
    }


    public static JogoResponseDTO fromJogo(Jogo jogo) {
        JogoResponseDTO dto = new JogoResponseDTO();
        dto.setIdJogo(jogo.getIdJogo());
        dto.setNomeJogo(jogo.getNomeJogo());
        dto.setGeneroJogo(jogo.getGeneroJogo());
        dto.setPlataformaJogo(jogo.getPlataformaJogo());
        dto.setAnoLancamentoJogo(jogo.getAnoLancamentoJogo());
        dto.setHorasJogadas(jogo.getHorasJogadas());
        dto.setStatusJogo(jogo.getStatusJogo());
        dto.setNotaJogo(jogo.getNotaJogo());
        dto.setDesenvolvedoraJogo(jogo.getDesenvolvedoraJogo());
        dto.setCategorias(jogo.getCategorias().stream().map(Categoria::getNome).sorted().toList());
        dto.setSteamAppId(jogo.getSteamAppId());
        dto.setImagemBanner(jogo.getImagemBanner());
        if (jogo.getSteamAppId() != null) {
            dto.setImagemCapa(String.format(URL_CAPA_STEAM, jogo.getSteamAppId()));
        }
        dto.setPlatina(jogo.getPlatina());

        return dto;

    }
}


