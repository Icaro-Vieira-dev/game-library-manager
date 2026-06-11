package com.gamelibrary.gamelibrarymanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import com.gamelibrary.gamelibrarymanager.model.Platina;

public class JogoResponseDTO {
    private Long idJogo;
    private String nomeJogo;
    private String generoJogo;
    private String plataformaJogo;
    private int anoLancamentoJogo;
    private double horasJogadas;
    private GameStatus statusJogo;
    private double notaJogo;
    private String desenvolvedoraJogo;

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
        dto.setPlatina(jogo.getPlatina());

        return dto;

    }
}


