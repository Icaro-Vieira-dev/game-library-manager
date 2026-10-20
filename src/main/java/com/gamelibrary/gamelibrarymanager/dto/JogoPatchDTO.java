package com.gamelibrary.gamelibrarymanager.dto;

import com.gamelibrary.gamelibrarymanager.model.StatusJogo;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Year;

// Body do PATCH /jogos/{id}: TODOS os campos são opcionais.
// Campo que não vier (null) = "não mexa nesse campo". Campo que vier = é validado e atualizado.
//
// As regras são as mesmas do POST/PUT, mas sem @NotBlank/@NotNull (aqui nada é obrigatório).
// Para textos, o @Pattern(".*\\S.*") faz o papel do @NotBlank só quando o campo vier:
// "precisa ter pelo menos um caractere que não seja espaço". Com null, o @Pattern não é checado.
public record JogoPatchDTO(

        @Pattern(regexp = ".*\\S.*", message = "O campo nome não pode estar vazio")
        String nomeJogo,

        @Pattern(regexp = ".*\\S.*", message = "O campo genero não pode estar vazio!")
        String generoJogo,

        @Pattern(regexp = ".*\\S.*", message = "O campo Plataforma não pode estar vazio")
        String plataformaJogo,

        @Min(value = 1958, message = "Lançamento do primeiro jogo foi em 1958, por isso aceitamos jogos com no mínimo esse ano!")
        Integer anoLancamentoJogo,

        @PositiveOrZero(message = "Horas jogadas não podem ser negativas.")
        Double horasJogadas,

        StatusJogo statusJogo,

        @DecimalMin(value = "1", message = "A nota deve ser entre 1 e 10.")
        @DecimalMax(value = "10", message = "A nota deve ser entre 1 e 10.")
        Double notaJogo,

        String desenvolvedoraJogo
) {
    // mesma regra e mesmo easter egg do JogoRequestDTO
    @AssertTrue(message = "Está prevendo o futuro amigão ?")
    public boolean isAnoLancamentoNoLimite() {
        return anoLancamentoJogo == null || anoLancamentoJogo <= Year.now().getValue() + 5;
    }
}
