package com.gamelibrary.gamelibrarymanager.dto;

import com.gamelibrary.gamelibrarymanager.model.StatusJogo;
import com.gamelibrary.gamelibrarymanager.model.Jogo;
import jakarta.validation.constraints.*;

import java.time.Year;

// Body do POST /jogos e do PUT /jogos/{id}.
// Num record, as anotações de validação ficam direto em cada componente (o "campo" entre parênteses).
public record JogoRequestDTO(
        @NotBlank(message = "O campo nome não pode estar vazio")
        String nomeJogo,

        @NotBlank(message = "O campo genero não pode estar vazio!")
        String generoJogo,

        @NotBlank(message = "O campo Plataforma não pode estar vazio")
        String plataformaJogo,

        // opcional (Integer pode ser null): um jogo PLANEJADO pode ainda não ter data
        @Min(value = 1958, message = "Lançamento do primeiro jogo foi em 1958, por isso aceitamos jogos com no mínimo esse ano!")
        Integer anoLancamentoJogo,

        // 0 é válido: jogo que você ainda não jogou. Double (e não double): se não vier no JSON,
        // chega como null e vira 0 no toJogo() (no Jackson 3, um primitivo ausente faria a leitura falhar)
        @PositiveOrZero(message = "Horas jogadas não podem ser negativas.")
        Double horasJogadas,

        @NotNull(message = "Não pode ser vazio, valores aceitos são: JOGANDO, FINALIZADO, ABANDONADO, PLANEJADO")
        StatusJogo statusJogo,

        // opcional (Double pode ser null): jogo ainda sem nota. Se vier, vai de 1 a 10.
        @DecimalMin(value = "1", message = "A nota deve ser entre 1 e 10.")
        @DecimalMax(value = "10", message = "A nota deve ser entre 1 e 10.")
        Double notaJogo,

        String desenvolvedoraJogo
) {
    // jogos anunciados podem ter lançamento até esse número de anos à frente
    private static final int ANOS_NO_FUTURO_PERMITIDOS = 5;

    // o limite acompanha o ano atual (em 2027 aceita até 2032), em vez de um número fixo no código
    // easter egg do projeto (assim como o do 1958): manter a mensagem
    @AssertTrue(message = "Está prevendo o futuro amigão ?")
    public boolean isAnoLancamentoNoLimite() {
        return anoLancamentoJogo == null || anoLancamentoJogo <= Year.now().getValue() + ANOS_NO_FUTURO_PERMITIDOS;
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
                this.horasJogadas == null ? 0 : this.horasJogadas,
                this.notaJogo == null ? 0 : this.notaJogo
        );
    }
}
