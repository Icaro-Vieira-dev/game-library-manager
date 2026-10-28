package com.gamelibrary.gamelibrarymanager;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

// Proteção contra CSRF pela checagem do cabeçalho Origin (o substituto do csrf() do Spring; veja SegurancaConfig).
//
// CSRF: um site malicioso faz o navegador da vítima mandar um POST para esta API, e o navegador anexa o
// cookie de login sozinho. O navegador SEMPRE informa de onde a requisição saiu no cabeçalho Origin,
// e o site malicioso não consegue mudar esse valor. Então, em toda requisição que altera dados:
//   - Origin igual ao front permitido (CORS_ORIGEM) ou à própria API (Swagger) -> segue;
//   - Origin "null" (iframe isolado, arquivo local, alguns redirecionamentos) -> 403;
//   - qualquer outro Origin -> 403;
//   - sem Origin -> segue: é um cliente fora do navegador (Postman, curl), que não sofre CSRF porque não
//     tem cookie da vítima para anexar. Navegadores atuais sempre mandam Origin em POST/PUT/PATCH/DELETE.
// Comparação exata de texto (equals): "http://localhost:4200.site-malicioso.com" não pode passar.
//
// NÃO é @Component de propósito: se fosse, o Spring Boot o registraria também como filtro solto do
// servidor (rodaria duas vezes e fora da cadeia de segurança). Ele é criado no SegurancaConfig.
public class FiltroOrigem extends OncePerRequestFilter {

    // GET, HEAD e OPTIONS não alteram dados: ficam de fora
    private static final Set<String> METODOS_QUE_ALTERAM = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final String origemPermitida;

    public FiltroOrigem(String origemPermitida) {
        this.origemPermitida = origemPermitida;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origem = request.getHeader(HttpHeaders.ORIGIN);

        boolean precisaConferir = METODOS_QUE_ALTERAM.contains(request.getMethod()) && origem != null;
        if (precisaConferir && !origemAceita(origem, request)) {
            recusar(response);
            return; // a requisição para aqui: não chega na autenticação nem no controller
        }
        chain.doFilter(request, response);
    }

    // "null" nunca é igual a nenhuma das duas (as duas começam com http:// ou https://), então cai no 403
    private boolean origemAceita(String origem, HttpServletRequest request) {
        return origem.equals(origemPermitida) || origem.equals(origemDaPropriaApi(request));
    }

    // scheme://host[:porta] da própria API, do jeito que o navegador escreve o Origin: SEM a porta quando
    // ela é a padrão (80 no http, 443 no https). Ex.: "http://localhost:8080" para o Swagger.
    // O host vem do cabeçalho Host, que um cliente fora do navegador pode forjar; isso não abre CSRF,
    // porque o navegador da VÍTIMA sempre manda o Host verdadeiro da API.
    private static String origemDaPropriaApi(HttpServletRequest request) {
        String scheme = request.getScheme();
        int porta = request.getServerPort();
        boolean portaPadrao = ("http".equals(scheme) && porta == 80) || ("https".equals(scheme) && porta == 443);
        return scheme + "://" + request.getServerName() + (portaPadrao ? "" : ":" + porta);
    }

    // 403 em ProblemDetail. Texto fixo: não repete o Origin recebido.
    private static void recusar(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Acesso negado\",\"status\":403,"
                + "\"detail\":\"Origem não permitida.\"}");
    }
}
