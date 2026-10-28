package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.CookieAutenticacao;
import com.gamelibrary.gamelibrarymanager.dto.CadastroRequestDTO;
import com.gamelibrary.gamelibrarymanager.dto.LoginRequestDTO;
import com.gamelibrary.gamelibrarymanager.dto.LoginResponseDTO;
import com.gamelibrary.gamelibrarymanager.dto.TokenResponseDTO;
import com.gamelibrary.gamelibrarymanager.dto.UsuarioResponseDTO;
import com.gamelibrary.gamelibrarymanager.model.Usuario;
import com.gamelibrary.gamelibrarymanager.service.UsuarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;

@Tag(name = "Autenticação", description = "Cadastro, login (token no cookie httpOnly) e logout")
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UsuarioService usuarioService;
    private final CookieAutenticacao cookieAutenticacao;
    private final Clock relogio;

    @Autowired
    public AuthController(UsuarioService usuarioService, CookieAutenticacao cookieAutenticacao, Clock relogio) {
        this.usuarioService = usuarioService;
        this.cookieAutenticacao = cookieAutenticacao;
        this.relogio = relogio;
    }

    /**
     * POST /auth/cadastro
     * Cria um usuário. A senha é guardada só como hash (Argon2id), nunca como texto.
     * O e-mail é guardado em minúsculas ("Icaro@Email.com" = "icaro@email.com").
     * O Steam ID é opcional: é o SteamID64 PÚBLICO do perfil (nunca pedimos API key nem senha da Steam).
     *
     * Exemplo: POST http://localhost:8080/auth/cadastro
     * Body (JSON): { "nome": "Icaro Vieira", "email": "icaro@email.com", "senha": "uma-senha-forte",
     *                "steamId": "76561198000000000" }
     * Regras: nome obrigatório, até 100 caracteres (espaços nas pontas são removidos);
     *         e-mail válido, até 254 caracteres; senha de 8 a 128 caracteres;
     *         steamId opcional (pode faltar, vir null ou ""), mas se vier: 17 dígitos começando com 7656119.
     * Resposta: 201 com { id, nome, email, steamId, criadoEm } (nunca devolve a senha).
     *           400 se algum campo estiver inválido (o campo "erros" diz qual).
     *           409 se o e-mail já estiver cadastrado, ou se o Steam ID já estiver ligado a outra conta.
     */
    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@Valid @RequestBody CadastroRequestDTO dto) {
        Usuario usuario = usuarioService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponseDTO.deUsuario(usuario));
    }

    /**
     * POST /auth/login
     * Confere e-mail e senha e entrega o token JWT num COOKIE httpOnly chamado "token" (cabeçalho
     * Set-Cookie). O token NUNCA vem no corpo: o JavaScript do front não consegue lê-lo, então um XSS
     * não consegue roubá-lo. O navegador manda o cookie sozinho nas próximas chamadas
     * (o Angular usa withCredentials: true).
     * Clientes fora do navegador (Postman, curl) também podem pegar o token do Set-Cookie e mandar no
     * cabeçalho Authorization: Bearer <token>.
     *
     * Exemplo: POST http://localhost:8080/auth/login
     * Body (JSON): { "email": "icaro@email.com", "senha": "uma-senha-forte" }
     * Resposta: 200 com { expiraEm } e o cabeçalho
     *               Set-Cookie: token=...; Path=/; Max-Age=3600; HttpOnly; Secure; SameSite=Strict
     *           400 se faltar e-mail ou senha.
     *           401 "E-mail ou senha inválidos." (a mesma resposta para e-mail inexistente e senha errada).
     *           403 "Origem não permitida." se vier de um site que não é o front (FiltroOrigem).
     *           429 depois de 5 tentativas erradas em 15 minutos (do mesmo e-mail e do mesmo IP):
     *               bloqueia por 15 minutos; o cabeçalho Retry-After diz em quantos segundos tentar de novo.
     * Um cookie velho ou inválido que venha junto é ignorado: o login funciona normalmente.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto, HttpServletRequest request) {
        // getRemoteAddr(): o IP de quem abriu a conexão. NÃO usamos o cabeçalho X-Forwarded-For:
        // qualquer cliente pode mandar esse cabeçalho com o valor que quiser (e trocar de "IP" a cada
        // tentativa para fugir do limite).
        TokenResponseDTO login = usuarioService.login(dto, request.getRemoteAddr());

        // o cookie vence junto com o token (jwt.expiracao-minutos)
        Duration validade = Duration.between(relogio.instant(), login.expiraEm().toInstant());
        ResponseCookie cookie = cookieAutenticacao.criar(login.token(), validade);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponseDTO(login.expiraEm()));
    }

    /**
     * POST /auth/logout
     * Sai: manda o navegador apagar o cookie de login (mesmo nome e atributos, Max-Age=0).
     * Não precisa estar logado (sair duas vezes não é erro).
     *
     * LIMITAÇÃO: o JWT é stateless (o servidor não guarda quais tokens existem). Apagar o cookie tira o
     * token DESTE navegador, mas uma cópia do token (se alguém a tiver) continua valendo até vencer.
     * Uma lista de tokens bloqueados fica para uma próxima etapa.
     *
     * Exemplo: POST http://localhost:8080/auth/logout
     * Resposta: 204 (sem corpo) com o cabeçalho
     *               Set-Cookie: token=; Path=/; Max-Age=0; Expires=...; HttpOnly; Secure; SameSite=Strict
     *           403 "Origem não permitida." se vier de um site que não é o front (FiltroOrigem).
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieAutenticacao.apagar().toString())
                .build();
    }
}
