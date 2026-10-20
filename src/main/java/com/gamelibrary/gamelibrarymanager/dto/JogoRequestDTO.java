package com.gamelibrary.gamelibrarymanager.dto;

import com.gamelibrary.gamelibrarymanager.model.GameStatus;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import jakarta.validation.constraints.*;

import java.time.Year;

public class JogoRequestDTO {
    // jogos anunciados podem ter lançamento até esse número de anos à frente
    private static final int ANOS_NO_FUTURO_PERMITIDOS = 5;

    @NotBlank(message = "O campo nome não pode estar vazio")
    private String nomeJogo;

    @NotBlank(message = "O campo genero não pode estar vazio!")
    private String generoJogo;

    @NotBlank(message = "O campo Plataforma não pode estar vazio")
    private String plataformaJogo;

    // opcional (Integer pode ser null): um jogo PLANEJADO pode ainda não ter data
    @Min(value = 1958, message = "Lançamento do primeiro jogo foi em 1958, por isso aceitamos jogos com no mínimo esse ano!")
    private Integer anoLancamentoJogo;

    // 0 é válido: jogo que você ainda não jogou
    @PositiveOrZero(message = "Horas jogadas não podem ser negativas.")
    private double horasJogadas;

    @NotNull(message = "Não pode ser vazio, valores aceitos são: JOGANDO, FINALIZADO, ABANDONADO, PLANEJADO")
    private GameStatus statusJogo;

    // opcional (Double pode ser null): jogo ainda sem nota. Se vier, vai de 1 a 10.
    @DecimalMin(value = "1", message = "A nota deve ser entre 1 e 10.")
    @DecimalMax(value = "10", message = "A nota deve ser entre 1 e 10.")
    private Double notaJogo;

    private String desenvolvedoraJogo;

    // o limite acompanha o ano atual (em 2027 aceita até 2032), em vez de um número fixo no código
    // easter egg do projeto (assim como o do 1958): manter a mensagem
    @AssertTrue(message = "Está prevendo o futuro amigão ?")
    public boolean isAnoLancamentoNoLimite() {
        return anoLancamentoJogo == null || anoLancamentoJogo <= Year.now().getValue() + ANOS_NO_FUTURO_PERMITIDOS;
    }


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

    public Integer getAnoLancamentoJogo() {
        return anoLancamentoJogo;
    }

    public double getHorasJogadas() {
        return horasJogadas;
    }

    public GameStatus getStatusJogo() {
        return statusJogo;
    }

    public Double getNotaJogo() {
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

    public void setAnoLancamentoJogo(Integer anoLancamentoJogo) {
        this.anoLancamentoJogo = anoLancamentoJogo;
    }

    public void setHorasJogadas(double horasJogadas) {
        this.horasJogadas = horasJogadas;
    }

    public void setStatusJogo(GameStatus statusJogo) {
        this.statusJogo = statusJogo;
    }

    public void setNotaJogo(Double notaJogo) {
        this.notaJogo = notaJogo;
    }

    public void setDesenvolvedoraJogo(String desenvolvedoraJogo) {
        this.desenvolvedoraJogo = desenvolvedoraJogo;
    }


    // metodo para converter o DTO em objeto Jogo
    // No banco, ano 0 = "sem data" e nota 0 = "sem nota" (mesmo padrão dos jogos importados da Steam)
    public Jogo toJogo() {
        return new Jogo(
                this.nomeJogo,
                this.statusJogo,
                this.generoJogo,
                this.plataformaJogo,
                this.desenvolvedoraJogo,
                this.anoLancamentoJogo == null ? 0 : this.anoLancamentoJogo,
                this.horasJogadas,
                this.notaJogo == null ? 0 : this.notaJogo
        );
    }
}


