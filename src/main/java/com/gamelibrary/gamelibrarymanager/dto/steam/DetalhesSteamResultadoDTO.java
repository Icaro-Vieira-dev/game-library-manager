package com.gamelibrary.gamelibrarymanager.dto.steam;

// Resumo devolvido pelo POST /importSteam/detalhes
public class DetalhesSteamResultadoDTO {
    // quantos jogos foram consultados nesta chamada
    private int processados;
    // quantos receberam gênero/categorias/desenvolvedora/ano
    private int atualizados;
    // quantos não estão mais na loja (removidos) e foram marcados para não buscar de novo
    private int semDadosNaLoja;
    // quantos ainda faltam buscar (chame de novo para continuar)
    private long restantes;
    private String mensagem;

    public DetalhesSteamResultadoDTO(int processados, int atualizados, int semDadosNaLoja, long restantes, String mensagem) {
        this.processados = processados;
        this.atualizados = atualizados;
        this.semDadosNaLoja = semDadosNaLoja;
        this.restantes = restantes;
        this.mensagem = mensagem;
    }

    // Getter
    public int getProcessados() {
        return processados;
    }
    public int getAtualizados() {
        return atualizados;
    }
    public int getSemDadosNaLoja() {
        return semDadosNaLoja;
    }
    public long getRestantes() {
        return restantes;
    }
    public String getMensagem() {
        return mensagem;
    }
}
