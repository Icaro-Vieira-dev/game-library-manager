package com.gamelibrary.gamelibrarymanager.model;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
// o mesmo jogo da Steam não entra duas vezes na biblioteca da MESMA pessoa; pessoas diferentes podem ter
// o mesmo jogo (por isso o par usuario_id + steam_app_id é que é único, e não o steam_app_id sozinho).
// Jogos manuais têm steam_app_id null, e NULL não conta como repetido.
@Table(name = "Jogos", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "steam_app_id"}))
public class Jogo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idJogo;

    // o DONO do jogo. Todo jogo tem um (NOT NULL), e cada usuário só enxerga os próprios.
    // LAZY: o usuário só é buscado no banco se alguém pedir algo além do id.
    // Nunca vai para o JSON (o JogoResponseDTO não tem esse campo).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    private String nomeJogo;
    private String generoJogo;
    private String plataformaJogo;
    private int anoLancamentoJogo;
    private double horasJogadas;
    @Enumerated(EnumType.STRING)
    private StatusJogo statusJogo;
    private double notaJogo;
    private String desenvolvedoraJogo;
    // null para jogos cadastrados manualmente. Único POR USUÁRIO (veja o @Table acima)
    private Long steamAppId;
    // controle interno: true depois que gênero/desenvolvedora/ano foram buscados na loja da Steam
    // (null = ainda não buscou). Evita buscar de novo o mesmo jogo a cada POST /steam/detalhes.
    private Boolean detalhesLojaBuscados;
    // URL do banner horizontal (460x215) da loja da Steam; preenchido pelo POST /steam/detalhes.
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

    public Jogo(String nomeJogo, StatusJogo status, String generoJogo, String plataformaJogo, String desenvolvedoraJogo, int anoLancamentoJogo, double horasJogadas, double notaJogo) {
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

    public StatusJogo getStatusJogo() {
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

    public Usuario getUsuario() {
        return usuario;
    }

    // usado só pelos services, sempre com o usuário logado (nunca com um id vindo da requisição)
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
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

    public void setStatusJogo(StatusJogo statusJogo) {
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

