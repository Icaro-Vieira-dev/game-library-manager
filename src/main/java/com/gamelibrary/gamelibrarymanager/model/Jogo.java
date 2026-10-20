package com.gamelibrary.gamelibrarymanager.model;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

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
    // null para jogos cadastrados manualmente; único para não importar o mesmo jogo da Steam duas vezes
    @Column(unique = true)
    private Long steamAppId;
    // controle interno: true depois que gênero/desenvolvedora/ano foram buscados na loja da Steam
    // (null = ainda não buscou). Evita buscar de novo o mesmo jogo a cada POST /importSteam/detalhes.
    private Boolean detalhesLojaBuscados;
    // URL do banner horizontal (460x215) da loja da Steam; preenchido pelo POST /importSteam/detalhes.
    // Guarda só o endereço: a imagem continua hospedada na Steam.
    @Column(length = 500)
    private String imagemBanner;
    // opcional: null enquanto você não definir uma platina para o jogo
    @Embedded
    private Platina platina;

    // um jogo pode ter várias categorias (ex.: Ação + RPG) e uma categoria tem vários jogos.
    // O vínculo fica na tabela jogo_categorias (jogo_id, categoria_id).
    // Set e não List: não deixa a mesma categoria entrar duas vezes no mesmo jogo.
    @ManyToMany
    @JoinTable(name = "jogo_categorias",
            joinColumns = @JoinColumn(name = "jogo_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_id"))
    private Set<Categoria> categorias = new HashSet<>();

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

    public Long getSteamAppId() {
        return steamAppId;
    }

    public void setSteamAppId(Long steamAppId) {
        this.steamAppId = steamAppId;
    }

    public Boolean getDetalhesLojaBuscados() {
        return detalhesLojaBuscados;
    }

    public void setDetalhesLojaBuscados(Boolean detalhesLojaBuscados) {
        this.detalhesLojaBuscados = detalhesLojaBuscados;
    }

    public String getImagemBanner() {
        return imagemBanner;
    }

    public void setImagemBanner(String imagemBanner) {
        this.imagemBanner = imagemBanner;
    }

    public Set<Categoria> getCategorias() {
        return categorias;
    }

    // add/remove devolvem false se não mudou nada (já tinha / não tinha a categoria)
    public boolean adicionarCategoria(Categoria categoria) {
        return categorias.add(categoria);
    }

    public boolean removerCategoria(Categoria categoria) {
        return categorias.remove(categoria);
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

