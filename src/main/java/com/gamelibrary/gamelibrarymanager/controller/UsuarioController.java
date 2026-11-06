package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.UsuarioLogado;
import com.gamelibrary.gamelibrarymanager.dto.AtualizarUsuarioRequestDTO;
import com.gamelibrary.gamelibrarymanager.dto.UsuarioResponseDTO;
import com.gamelibrary.gamelibrarymanager.model.Usuario;
import com.gamelibrary.gamelibrarymanager.service.UsuarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuários", description = "Dados do usuário logado (exige login)")
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
     * O usuário vem de DENTRO do token (o "sub", que o Spring já verificou, pelo @UsuarioLogado), NUNCA de
     * um parâmetro da requisição: assim ninguém consegue pedir os dados de outra pessoa trocando um número na URL.
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
    public UsuarioResponseDTO eu(@UsuarioLogado Usuario usuario) {
        return UsuarioResponseDTO.deUsuario(usuario);
    }

    /**
     * PATCH /usuarios/me
     * Troca o Steam ID do usuário logado (usado pelo POST /steam/importacao e pela platina via Steam).
     * Por enquanto é a única coisa que dá para trocar aqui (nome, e-mail e senha ficam para outra etapa).
     * O usuário vem do token, NUNCA do body: não adianta mandar "id" no body.
     * É só o ID PÚBLICO do perfil: nunca pedimos API key nem senha da Steam.
     *
     * Exemplos: PATCH http://localhost:8080/usuarios/me
     *   Cadastrar ou trocar: { "steamId": "76561198000000000" }
     *   Remover:             { "steamId": null }  ou  { "steamId": "" }
     *   ATENÇÃO: um body vazio {} também REMOVE o Steam ID (sem o campo, ele conta como null).
     * Regras: o mesmo formato do cadastro (17 dígitos começando com 7656119); espaços nas pontas são removidos.
     * Resposta: 200 com { id, nome, email, steamId, criadoEm }.
     *           400 se o Steam ID for inválido. 401 sem login.
     *           409 "Este Steam ID já está ligado a outra conta."
     */
    @PatchMapping("/me")
    public UsuarioResponseDTO atualizar(@Valid @RequestBody AtualizarUsuarioRequestDTO dto, @UsuarioLogado Usuario usuario) {
        return UsuarioResponseDTO.deUsuario(usuarioService.atualizarSteamId(usuario, dto.steamId()));
    }
}
