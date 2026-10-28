package com.gamelibrary.gamelibrarymanager.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Locale;

// Um usuário da aplicação (quem faz login).
// Tabela "usuarios", e não "user": "user" é palavra reservada no Postgres.
@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // guardado já normalizado (veja normalizarEmail). UNIQUE no banco: mesmo que o código deixe
    // passar um cadastro repetido (dois ao mesmo tempo), o banco recusa.
    // 254 = tamanho máximo de um endereço de e-mail válido.
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    // nome de exibição, ex.: "Icaro Vieira"
    @Column(nullable = false, length = 100)
    private String nome;

    // SteamID64 do usuário (17 dígitos, ex.: 76561198000000000). Opcional: null se não informou.
    // UNIQUE: duas contas não podem usar o mesmo Steam ID. Várias contas SEM Steam ID (null) podem
    // existir juntas: no banco, NULL não conta como valor repetido.
    // Só o ID PÚBLICO da Steam: nunca guardamos API key nem senha da Steam do usuário.
    @Column(unique = true, length = 17)
    private String steamId;

    // NUNCA a senha: só o hash Argon2id dela (ex.: "$argon2id$v=19$m=16384,t=2,p=1$...").
    // Quem gera e confere é só o UsuarioService.
    @Column(nullable = false)
    private String senhaHash;

    @Column(nullable = false)
    private OffsetDateTime criadoEm;

    // construtor
    // o JPA exige um construtor sem argumentos (ele cria o objeto vazio e preenche ao ler do banco)
    protected Usuario() {}
    public Usuario(String nome, String email, String steamId, String senhaHash, OffsetDateTime criadoEm) {
        this.nome = nome;
        this.email = normalizarEmail(email);
        this.steamId = steamId;
        this.senhaHash = senhaHash;
        this.criadoEm = criadoEm;
    }

    // O mesmo e-mail escrito de jeitos diferentes conta como um só:
    // "  Icaro@Email.com " -> "icaro@email.com".
    // Locale.ROOT: deixa o toLowerCase igual em qualquer máquina (em turco, por exemplo, "I" não vira "i").
    public static String normalizarEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    // Getter
    // Sem setters: não existe "setSenha". Trocar senha (próximas etapas) vai passar pelo UsuarioService,
    // que é o único lugar que gera hash.
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNome() {
        return nome;
    }

    public String getSteamId() {
        return steamId;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    // Só o id: se alguém logar um Usuario por engano, o hash e o e-mail não vão junto
    @Override
    public String toString() {
        return "Usuario[id=" + id + "]";
    }
}
