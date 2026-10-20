package com.gamelibrary.gamelibrarymanager.model;

import jakarta.persistence.*;

import java.time.Instant;

// O perfil da Steam ligado à biblioteca. Existe no máximo UM (a biblioteca é de uma pessoa só),
// por isso o id é sempre 1 e não é gerado pelo banco: salvar de novo substitui o anterior.
// É criado na primeira importação e usado depois na sincronização e nas conquistas,
// para o usuário não precisar informar o Steam ID toda vez.
@Entity
@Table(name = "perfil_steam")
public class PerfilSteam {
    public static final long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    // SteamID64: 17 dígitos, começa com 7656
    @Column(nullable = false, length = 17)
    private String steamId;
    private String nome;
    private String avatar;
    private String urlPerfil;
    private Instant ultimaImportacao;

    // o JPA exige um construtor vazio
    public PerfilSteam() {}

    public PerfilSteam(String steamId) {
        this.steamId = steamId;
    }

    // Getter
    public Long getId() {
        return id;
    }

    public String getSteamId() {
        return steamId;
    }

    public String getNome() {
        return nome;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getUrlPerfil() {
        return urlPerfil;
    }

    public Instant getUltimaImportacao() {
        return ultimaImportacao;
    }

    // Setter
    public void setSteamId(String steamId) {
        this.steamId = steamId;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setUrlPerfil(String urlPerfil) {
        this.urlPerfil = urlPerfil;
    }

    public void setUltimaImportacao(Instant ultimaImportacao) {
        this.ultimaImportacao = ultimaImportacao;
    }
}
