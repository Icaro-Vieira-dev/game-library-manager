package com.gamelibrary.gamelibrarymanager.model;
import jakarta.persistence.*;

@Entity
@Table(name = "Jogos")
public class Jogo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idJogo;
    private String nomeJogo;
    private String generoJogo;
    private String plataformaJogo;
    private int anoLancamentoJogo;
    private double horasJogadas;
    @Enumerated(EnumType.STRING)
    private GameStatus statusJogo;
    private double notaJogo;
    private String desenvolvedoraJogo;
    @Transient
    private Platina platina;


    public Jogo (){}

    public Jogo(String nomeJogo, GameStatus status, String generoJogo, String plataformaJogo, String desenvolvedoraJogo, int anoLancamentoJogo, double horasJogadas, double notaJogo) {
        this.nomeJogo = nomeJogo;
        this.generoJogo = generoJogo;
        this.plataformaJogo = plataformaJogo;
        this.anoLancamentoJogo = anoLancamentoJogo;
        this.horasJogadas = horasJogadas;
        this.statusJogo = status;
        this.notaJogo = notaJogo;
        this.desenvolvedoraJogo = desenvolvedoraJogo;
    }

    // Getters.
    public String getNomeJogo() {
        return nomeJogo;
    }

    public String getGeneroJogo() {
        return generoJogo;
    }

    public String getPlataformaJogo() {
        return plataformaJogo;
    }

    public double getHorasJogadas() {
        return horasJogadas;
    }

    public int getAnoLancamentoJogo() {
        return anoLancamentoJogo;
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

    public Long getIdJogo() {
        return idJogo;
    }

    public Platina getPlatina() {
        return platina;
    }

    //Setters.


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

    public void setIdJogo(Long idJogo) {this.idJogo = idJogo;}

    public void setPlatina(Platina platina) {
        this.platina = platina;
    }
}

