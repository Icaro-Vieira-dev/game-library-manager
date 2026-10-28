package com.gamelibrary.gamelibrarymanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Body do POST /auth/cadastro:
// { "nome": "Icaro Vieira", "email": "icaro@email.com", "senha": "uma-senha-forte", "steamId": "76561198000000000" }
// steamId é opcional (pode faltar, vir null ou vazio).
// Só carrega e valida. O hash da senha é feito no UsuarioService (o único lugar que mexe com senha).
public record CadastroRequestDTO(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "O e-mail pode ter no máximo 254 caracteres.")
        String email,

        // mínimo 8 (recomendação da OWASP para senha sem 2º fator); máximo 128 para ninguém mandar
        // um texto gigante e fazer o Argon2 (que é lento de propósito) travar o servidor.
        // Os espaços NÃO são removidos: fazem parte da senha.
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 128, message = "A senha precisa ter entre 8 e 128 caracteres.")
        String senha,

        // SteamID64: 17 dígitos, sempre começando com 7656119. O @Pattern só confere quando o campo vem
        // preenchido (null passa). É o ID PÚBLICO do perfil: nunca pedimos API key nem senha da Steam.
        @Pattern(regexp = "^7656119\\d{10}$",
                message = "Steam ID inválido: use o SteamID64, com 17 dígitos começando com 7656119 "
                        + "(aparece em steamcommunity.com/profiles/7656... ou em https://steamid.io).")
        String steamId
) {
    // Construtor compacto: roda quando o JSON é lido, ANTES da validação (@NotBlank, @Pattern...).
    //   - nome: tira os espaços das pontas ("  Icaro " -> "Icaro"); só espaços vira "" e o @NotBlank recusa;
    //   - steamId: tira os espaços das pontas; vazio ou só espaços vira null (= "não informou").
    //     Sem isso, o "" que alguns front-ends mandam seria recusado pelo @Pattern.
    // email e senha NÃO são mexidos aqui (a senha nunca é alterada; o e-mail é normalizado no service).
    public CadastroRequestDTO {
        nome = nome == null ? null : nome.strip();
        steamId = (steamId == null || steamId.isBlank()) ? null : steamId.strip();
    }

    // O toString() padrão de um record mostra todos os campos, inclusive a senha.
    // Se alguém logar este objeto por engano, a senha não aparece.
    @Override
    public String toString() {
        return "CadastroRequestDTO[nome=" + nome + ", email=" + email + ", senha=***, steamId=" + steamId + "]";
    }
}
