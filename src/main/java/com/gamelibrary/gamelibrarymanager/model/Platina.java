package com.gamelibrary.gamelibrarymanager.model;

public class Platina {
    private boolean platinaAtiva;
    private int conquistaObtida;
    private int totalConquista;


    public Platina(boolean platinaAtiva,  int conquistaObtida, int totalConquista) {
        this.platinaAtiva = platinaAtiva;
        this.conquistaObtida = conquistaObtida;
        this.totalConquista = totalConquista;
    }

    // Getter.
    public int getTotalConquista() {
        return totalConquista;
    }

    public boolean isPlatinaAtiva() {
        return platinaAtiva;
    }

    public int getConquistaObtida() {
        return conquistaObtida;
    }

    public int getConquistaRestante(){
        return totalConquista - conquistaObtida;
    }


    // Setters.

    public void setPlatinaAtiva(boolean platinaAtiva) {
        this.platinaAtiva = platinaAtiva;
    }

    public void setConquistaObtida(int conquistaObtida) {
        this.conquistaObtida = conquistaObtida;
    }

    public void setTotalConquista(int totalConquista) {
        this.totalConquista = totalConquista;
    }
    


}
