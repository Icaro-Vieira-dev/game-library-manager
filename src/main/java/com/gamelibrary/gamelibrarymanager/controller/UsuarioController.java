package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.dto.UsuarioResponseDTO;
import com.gamelibrary.gamelibrarymanager.service.UsuarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuários", description = "Dados do usuário logado (precisa de token)")
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * GET /usuarios/me
     * Devolve o usuário logado. O front chama esta rota para saber quem está logado.
     * Sem login, ou com token inválido/vencido, a requisição nem chega aqui.
     * O id vem de DENTRO do token (o "sub", que o Spring já verificou), NUNCA de um parâmetro da
     * requisição: assim ninguém consegue pedir os dados de outra pessoa trocando um número na URL.
     *
     * O token pode vir de dois jeitos (se vierem os dois, vale o cookie):
     *   - cookie "token" (o navegador manda sozinho depois do POST /auth/login; o Angular usa
     *     withCredentials: true);
     *   - cabeçalho Authorization: Bearer <token> (Swagger, Postman).
     *
     * Exemplo: GET http://localhost:8080/usuarios/me
     * Resposta: 200 com { id, nome, email, steamId, criadoEm } (steamId null se não foi informado).
     *           401 sem login, com token inválido, alterado, de outro emissor, vencido,
     *               ou de um usuário que não existe mais. Se o token veio no cookie, a resposta também
     *               manda apagar o cookie (Set-Cookie com Max-Age=0).
     */
    @GetMapping("/me")
    public UsuarioResponseDTO eu(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponseDTO.deUsuario(usuarioService.buscarPorId(idDoToken(jwt)));
    }

    // o "sub" é o id do usuário (veja UsuarioService.gerarToken)
    private static Long idDoToken(Jwt jwt) {
        if (jwt == null) {
            throw new InvalidBearerTokenException("Token inválido.");
        }
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidBearerTokenException("Token inválido.");
        }
    }
}
