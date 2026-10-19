package com.gamelibrary.gamelibrarymanager.model;

import jakarta.persistence.Embeddable;

// Fica "embutida" no Jogo: as colunas vão para a própria tabela jogos.
// Tudo é tipo objeto (Boolean/Integer) para poder ser null: jogo sem platina definida = platina null.
@Embeddable
public class Platina {
    // true = você tem a meta de platinar este jogo; false = desistiu da meta
    private Boolean platinaAtiva;
    private Integer conquistaObtida;
    private Integer totalConquista;

    // o JPA exige um construtor vazio
    public Platina() {}

    public Platina(Boolean platinaAtiva, Integer conquistaObtida, Integer totalConquista) {
        this.platinaAtiva = platinaAtiva;
        this.conquistaObtida = conquistaObtida;
        this.totalConquista = totalConquista;
    }

    // Getter.
    public Boolean getPlatinaAtiva() {
        return platinaAtiva;
    }

    public Integer getConquistaObtida() {
        return conquistaObtida;
    }

    public Integer getTotalConquista() {
        return totalConquista;
    }

    // calculados: não são colunas, só aparecem no JSON de resposta
    public Integer getConquistaRestante(){
        if (conquistaObtida == null || totalConquista == null) {
            return null;
        }
        return totalConquista - conquistaObtida;
    }

    public boolean isPlatinado() {
        return totalConquista != null && totalConquista > 0 && totalConquista.equals(conquistaObtida);
    }


    // Setters.

    public void setPlatinaAtiva(Boolean platinaAtiva) {
        this.platinaAtiva = platinaAtiva;
    }

    public void setConquistaObtida(Integer conquistaObtida) {
        this.conquistaObtida = conquistaObtida;
    }

    public void setTotalConquista(Integer totalConquista) {
        this.totalConquista = totalConquista;
    }
}
