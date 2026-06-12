package com.gamelibrary.gamelibrarymanager.dto;

import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import jakarta.validation.constraints.*;

public class JogoRequestDTO {
    @NotBlank(message = "O campo nome não pode estar vazio")
    private String nomeJogo;

    @NotBlank(message = "O campo genero não pode estar vazio!")
    private String generoJogo;

    @NotBlank(message = "O campo Plataforma não pode estar vazio")
    private String plataformaJogo;

    @Min(value = 1958, message = "Lançamento do primeiro jogo foi em 1958, por isso aceitamos jogos com no mínimo esse ano!")
    @Max(value = 2026, message = "Está prevendo o futuro amigão ?")
    private int anoLancamentoJogo;

    @Positive(message = "Campo horas jogadas devem ser horas positivas.")
    private double horasJogadas;

    @NotNull(message = "Não pode ser vazio, valores aceitos são: JOGANDO, FINALIZADO, ABANDONADO, PLANEJADO")
    private GameStatus statusJogo;

    @Positive(message = "A nota não pode ser negativas")
    @Min(value = 1, message = "A nota deve ser 1 ou 10")
    @Max(value = 10, message = "A nota maxima é 10")
    private double notaJogo;

    private String desenvolvedoraJogo;


    //getters

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


    //setter

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


    // metodo para converter o DTO em objeto Jogo
    public Jogo toJogo() {
        return new Jogo(
                this.nomeJogo,
                this.statusJogo,
                this.generoJogo,
                this.plataformaJogo,
                this.desenvolvedoraJogo,
                this.anoLancamentoJogo,
                this.horasJogadas,
                this.notaJogo
        );
    }
}


