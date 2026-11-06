package com.gamelibrary.gamelibrarymanager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Categorias são POR USUÁRIO: o "RPG" do usuário A não é o "RPG" do usuário B.
// Único por usuário: o nome NORMALIZADO ("RPG", "rpg" e " Rpg " contam como o mesmo). O banco garante
// isso mesmo quando dois pedidos chegam ao mesmo tempo (o código só confere antes, e pode perder a corrida).
@Entity
@Table(name="Categoria", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "nome_normalizado"}))
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String nome;

    // nome sem espaços nas pontas e em minúsculas, só para comparar e buscar ("RPG" -> "rpg").
    // Preenchido pela própria entidade sempre que o nome muda: não tem setter público.
    @Column(name = "nome_normalizado", nullable = false)
    private String nomeNormalizado;

    // o DONO da categoria (NOT NULL). LAZY: só busca o usuário se alguém pedir algo além do id.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // lado "inverso" do vínculo: quem manda é o Jogo.categorias (mappedBy aponta para ele)
    @ManyToMany(mappedBy = "categorias")
    private List<Jogo> jogos = new ArrayList<>();

    // construtor
    public Categoria() {}
    public Categoria(String nome, Usuario usuario) {
        setNome(nome);
        this.usuario = usuario;
    }

    // A regra de "mesmo nome" em um lugar só. Locale.ROOT: igual em qualquer máquina
    // (em turco, por exemplo, "I" não viraria "i").
    public static String normalizarNome(String nome) {
        return nome == null ? null : nome.strip().toLowerCase(Locale.ROOT);
    }

    // Getter
    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getNomeNormalizado() {
        return nomeNormalizado;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public List<Jogo> getJogos() {
        return jogos;
    }


    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    // troca o nome e, junto, o nome normalizado (os dois nunca ficam diferentes)
    public void setNome(String nome) {
        this.nome = nome;
        this.nomeNormalizado = normalizarNome(nome);
    }
}
