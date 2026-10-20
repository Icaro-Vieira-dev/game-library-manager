package com.gamelibrary.gamelibrarymanager.dto;

import com.gamelibrary.gamelibrarymanager.model.Platina;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

// Body do PUT /jogos/{id}/platina
public record PlatinaRequestDTO(
        @NotNull(message = "Informe platinaAtiva: true para ativar a meta de platinar, false para desativar.")
        Boolean platinaAtiva,

        @PositiveOrZero(message = "conquistaObtida não pode ser negativa.")
        Integer conquistaObtida,

        @Positive(message = "totalConquista deve ser maior que 0.")
        Integer totalConquista
) {
    // validações que envolvem mais de um campo: o @AssertTrue exige que o método devolva true.
    // Elas continuam como métodos "is...": é esse prefixo que faz o validador enxergá-las.

    @AssertTrue(message = "Informe conquistaObtida e totalConquista juntos, ou nenhum dos dois.")
    public boolean isConquistasInformadasJuntas() {
        return (conquistaObtida == null) == (totalConquista == null);
    }

    @AssertTrue(message = "conquistaObtida não pode ser maior que totalConquista.")
    public boolean isObtidasDentroDoTotal() {
        if (conquistaObtida == null || totalConquista == null) {
            return true; // esse caso é tratado na validação acima
        }
        return conquistaObtida <= totalConquista;
    }

    // metodo para converter o DTO em objeto Platina
    public Platina toPlatina() {
        return new Platina(this.platinaAtiva, this.conquistaObtida, this.totalConquista);
    }
}
