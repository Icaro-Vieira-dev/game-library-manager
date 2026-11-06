package com.gamelibrary.gamelibrarymanager;

import com.gamelibrary.gamelibrarymanager.model.Usuario;
import com.gamelibrary.gamelibrarymanager.service.UsuarioService;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

// Preenche os parâmetros "@UsuarioLogado Usuario usuario" dos controllers (registrado no WebConfig).
// É o mesmo mecanismo que o Spring usa para @PathVariable e @RequestBody.
//
// De onde vem o usuário: da autenticação que o Spring Security JÁ VERIFICOU (assinatura, emissor e
// validade do JWT), pelo "sub" do token. Este código nunca olha a URL, o body nem os parâmetros.
// Qualquer coisa fora do esperado (sem login, sub estranho, conta apagada) -> 401, nunca um usuário errado.
@Component
public class UsuarioLogadoResolver implements HandlerMethodArgumentResolver {

    private final UsuarioService usuarioService;

    public UsuarioLogadoResolver(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // só atende parâmetros do tipo Usuario marcados com @UsuarioLogado
    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return parametro.hasParameterAnnotation(UsuarioLogado.class) && Usuario.class.equals(parametro.getParameterType());
    }

    @Override
    public Usuario resolveArgument(MethodParameter parametro, ModelAndViewContainer mavContainer,
                                   NativeWebRequest requisicao, WebDataBinderFactory binderFactory) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        // numa rota pública não há JWT: se alguém usar @UsuarioLogado nela por engano, falha fechado (401)
        if (!(autenticacao instanceof JwtAuthenticationToken tokenJwt)) {
            throw new InvalidBearerTokenException("Token inválido.");
        }
        Long id;
        try {
            // o "sub" é o id do usuário (veja UsuarioService.gerarToken)
            id = Long.valueOf(tokenJwt.getToken().getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidBearerTokenException("Token inválido.");
        }
        // conta apagada depois do login -> 401 (e o GlobalExceptionHandler manda apagar o cookie)
        return usuarioService.buscarPorId(id);
    }
}
