package com.gamelibrary.gamelibrarymanager.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

// Uma conquista (achievement) da Steam de um jogo. É o "checklist" da platina.
@Entity
// a mesma conquista não pode aparecer duas vezes no mesmo jogo (sincronizar de novo atualiza, não duplica)
@Table(name = "conquistas", uniqueConstraints = @UniqueConstraint(columnNames = {"jogo_id", "api_name"}))
public class Conquista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: só carrega o jogo do banco se alguém chamar getJogo()
    // OnDelete CASCADE: apagar o jogo apaga as conquistas dele (direto no banco, vale até para o deleteAllInBatch)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogo_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Jogo jogo;

    // identificador da conquista na Steam (ex.: "ACH_FINISH_GAME"); não muda, diferente do nome
    @Column(name = "api_name", nullable = false)
    private String apiName;
    private String nome;
    @Column(length = 1000)
    private String descricao;
    private boolean concluida;
    // null enquanto não for desbloqueada
    private Instant dataDesbloqueio;

    public Conquista() {}

    public Conquista(Jogo jogo, String apiName) {
        this.jogo = jogo;
        this.apiName = apiName;
    }

    // Getter
    public Long getId() {
        return id;
    }

    public Jogo getJogo() {
        return jogo;
    }

    public String getApiName() {
        return apiName;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isConcluida() {
        return concluida;
    }

    public Instant getDataDesbloqueio() {
        return dataDesbloqueio;
    }

    // Setter
    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public void setConcluida(boolean concluida) {
        this.concluida = concluida;
    }

    public void setDataDesbloqueio(Instant dataDesbloqueio) {
        this.dataDesbloqueio = dataDesbloqueio;
    }
}
