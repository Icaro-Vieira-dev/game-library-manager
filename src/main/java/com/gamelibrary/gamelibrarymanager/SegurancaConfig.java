package com.gamelibrary.gamelibrarymanager;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

// Configuração de segurança da API: quem pode acessar o quê, como as senhas viram hash,
// como o JWT é assinado e verificado (e de onde ele é lido: cookie ou cabeçalho), CORS, CSRF
// e as respostas 401/403.
@Configuration
public class SegurancaConfig {

    // o "iss" (emissor) de todos os tokens. O JwtDecoder recusa token com outro emissor.
    public static final String EMISSOR_JWT = "game-library-manager";
    // HS256 exige uma chave de pelo menos 256 bits = 32 bytes
    private static final int TAMANHO_MINIMO_CHAVE_BYTES = 32;
    // texto fixo de todo 401 por falta de login (aqui e no GlobalExceptionHandler)
    public static final String MENSAGEM_NAO_AUTENTICADO =
            "É preciso fazer login (cookie de sessão ou cabeçalho Authorization: Bearer <token>).";

    private final SecretKey chaveJwt;
    private final String origemPermitida;
    private final CookieAutenticacao cookieAutenticacao;

    // As duas validações rodam quando a aplicação sobe: se a chave ou a origem do CORS estiverem
    // erradas, a aplicação NÃO SOBE (melhor falhar na hora do que rodar inseguro).
    public SegurancaConfig(@Value("${jwt.secret}") String segredoJwt,
                           @Value("${app.cors.origem-permitida}") String origemPermitida,
                           CookieAutenticacao cookieAutenticacao) {
        this.chaveJwt = criarChaveJwt(segredoJwt);
        this.origemPermitida = validarOrigem(origemPermitida);
        this.cookieAutenticacao = cookieAutenticacao;
    }

    // ---------------------------------------------------------------------------------------------
    // Senhas
    // ---------------------------------------------------------------------------------------------

    // Argon2id, o algoritmo recomendado pela OWASP para senhas. É lento e gasta memória (16 MB por
    // hash) de propósito: isso torna caro testar bilhões de senhas se o banco vazar.
    // Cada hash tem um "sal" aleatório próprio: duas pessoas com a mesma senha têm hashes diferentes.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    // ---------------------------------------------------------------------------------------------
    // JWT
    // ---------------------------------------------------------------------------------------------

    // Assina os tokens (usado no login, pelo UsuarioService).
    @Bean
    public JwtEncoder jwtEncoder() {
        return NimbusJwtEncoder.withSecretKey(chaveJwt).algorithm(MacAlgorithm.HS256).build();
    }

    // AQUI o JWT é VERIFICADO, em toda requisição que traz "Authorization: Bearer <token>".
    // O Spring confere, nesta ordem:
    //   1. a assinatura, com a mesma chave e SÓ com HS256 (um token com "alg": "none" ou outro
    //      algoritmo é recusado). Token alterado ou assinado com outra chave -> 401;
    //   2. os validadores abaixo. Qualquer um que falhar -> 401.
    @Bean
    public JwtDecoder jwtDecoder(Clock relogio) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();

        // exp: o token precisa TER validade (setAllowEmptyExpiryClaim(false); o padrão do Spring aceitaria
        // um token sem "exp", que valeria para sempre) e ela não pode ter passado.
        // Duration.ZERO = sem tolerância: quem emite e quem confere é o mesmo servidor, com o mesmo relógio.
        JwtTimestampValidator validadeNoTempo = new JwtTimestampValidator(Duration.ZERO);
        validadeNoTempo.setAllowEmptyExpiryClaim(false);
        validadeNoTempo.setClock(relogio);

        OAuth2TokenValidator<Jwt> validadores = new DelegatingOAuth2TokenValidator<>(
                // iss: precisa ser "game-library-manager"
                new JwtIssuerValidator(EMISSOR_JWT),
                validadeNoTempo,
                // sub: precisa existir (é o id do usuário, usado no GET /usuarios/me)
                new JwtClaimValidator<String>(JwtClaimNames.SUB, sub -> sub != null && !sub.isBlank()));
        decoder.setJwtValidator(validadores);
        return decoder;
    }

    // Transforma o JWT_SECRET em chave HS256. Se faltar ou for curto demais, a aplicação não sobe.
    // Usa os bytes do texto como está (o resultado do openssl em Base64 é um texto aleatório; não
    // precisa decodificar). A mensagem de erro NUNCA mostra a chave.
    private static SecretKey criarChaveJwt(String segredo) {
        byte[] bytes = segredo == null ? new byte[0] : segredo.strip().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_CHAVE_BYTES) {
            throw new IllegalStateException("JWT_SECRET não configurada ou curta demais (mínimo "
                    + TAMANHO_MINIMO_CHAVE_BYTES + " bytes). Gere uma chave aleatória com:  openssl rand -base64 48"
                    + "  e coloque no arquivo .env como JWT_SECRET=<resultado>. Depois reinicie a aplicação.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    // ---------------------------------------------------------------------------------------------
    // Regras de acesso
    // ---------------------------------------------------------------------------------------------

    @Bean
    public SecurityFilterChain filtroDeSeguranca(
            HttpSecurity http,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping rotasDaAplicacao) throws Exception {
        http
                // CSRF: o csrf() do Spring (token anti-CSRF em cada formulário) fica desligado, porque esta API
                // não tem formulários. Agora que o login vai num COOKIE (que o navegador manda sozinho), quem
                // protege contra CSRF são duas barreiras:
                //   a) SameSite=Strict no cookie (CookieAutenticacao): o navegador não manda o cookie quando a
                //      requisição começa em outro SITE;
                //   b) o FiltroOrigem: recusa POST/PUT/PATCH/DELETE vindo de uma origem que não é o front nem a API.
                // A (b) é a que protege de verdade: o SameSite olha o "site" (domínio), não a porta, então
                // localhost:4200 e localhost:8080 contam como o MESMO site, e qualquer coisa rodando em outra
                // porta do localhost passaria pelo SameSite.
                .csrf(csrf -> csrf.disable())
                // roda ANTES do CorsFilter (e portanto antes da autenticação): uma origem estranha recebe nosso
                // 403 em ProblemDetail. Depois dele, o CorsFilter recusaria do mesmo jeito, mas com texto puro.
                .addFilterBefore(new FiltroOrigem(origemPermitida), CorsFilter.class)
                // CORS: usa o bean corsConfigurationSource() abaixo
                .cors(Customizer.withDefaults())
                // sem sessão: cada requisição se identifica sozinha pelo token (nada fica guardado no servidor)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // sem login por formulário, sem "usuário:senha" no cabeçalho e sem o logout de sessão do Spring
                // (o nosso logout é o POST /auth/logout, que apaga o cookie)
                .httpBasic(basico -> basico.disable())
                .formLogin(formulario -> formulario.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(rotas -> rotas
                        // a página de erro interna do Spring (quando algo dá errado ao responder):
                        // sem isso, um erro numa rota pública viraria 401
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // cadastro, login e logout: públicos, mas SÓ com POST
                        // (outros métodos nessas rotas caem no "negar por padrão")
                        .requestMatchers(HttpMethod.POST, "/auth/cadastro", "/auth/login", "/auth/logout").permitAll()
                        // rotas que já existiam continuam públicas por enquanto
                        // (ligar cada jogo ao seu dono é a próxima etapa)
                        .requestMatchers("/jogos/**", "/categorias/**", "/steam/**", "/vitrine/**").permitAll()
                        // documentação (Swagger)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // só com token válido
                        .requestMatchers("/usuarios/**").authenticated()
                        // QUALQUER outra rota: negar por padrão (veja negarRotasNaoListadas)
                        .anyRequest().access(negarRotasNaoListadas(rotasDaAplicacao)))
                // JWT: pega o token (cookie ou cabeçalho, veja lerToken) e verifica com o jwtDecoder() acima
                .oauth2ResourceServer(servidor -> servidor
                        .bearerTokenResolver(lerToken())
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(respostaNaoAutenticado())
                        .accessDeniedHandler(respostaAcessoNegado()))
                // as mesmas respostas para quando nem veio token
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(respostaNaoAutenticado())
                        .accessDeniedHandler(respostaAcessoNegado()));
        return http.build();
    }

    // "Negar por padrão" para tudo que não está listado acima, com um cuidado:
    //   - se a rota EXISTE na aplicação (algum @GetMapping/@PostMapping... atende), ela é NEGADA:
    //     é uma rota nova que alguém esqueceu de liberar ou proteger -> 401 (sem token) ou 403;
    //   - se a rota NÃO EXISTE, a requisição segue e o Spring responde 404, como antes.
    // Por que não um denyAll() simples? Ele transformaria TODA rota inexistente em 401, quebrando o
    // contrato atual da API (os testes que já existem esperam 404 para rota inexistente). Para uma rota
    // que não existe não há nada a proteger; para uma que existe, a regra é negar.
    private static AuthorizationManager<RequestAuthorizationContext> negarRotasNaoListadas(
            RequestMappingHandlerMapping rotasDaAplicacao) {
        return (autenticacao, contexto) -> new AuthorizationDecision(!existeRota(rotasDaAplicacao, contexto.getRequest()));
    }

    // Pergunta ao Spring MVC se algum controller atende esta requisição.
    private static boolean existeRota(RequestMappingHandlerMapping rotasDaAplicacao, HttpServletRequest request) {
        // o MVC precisa do caminho da requisição já "lido"; aqui ainda estamos antes dele, então lemos
        // e, no fim, desfazemos (para não deixar nada diferente para o MVC)
        boolean leuOCaminho = !ServletRequestPathUtils.hasParsedRequestPath(request);
        if (leuOCaminho) {
            ServletRequestPathUtils.parseAndCache(request);
        }
        try {
            return rotasDaAplicacao.getHandler(request) != null;
        } catch (Exception e) {
            // ex.: o caminho existe, mas com outro método HTTP. NA DÚVIDA, NEGA: trata como rota existente.
            return true;
        } finally {
            if (leuOCaminho) {
                ServletRequestPathUtils.clearParsedRequestPath(request);
            }
        }
    }

    // De onde vem o token de cada requisição, nesta ordem:
    //   a) /auth/** -> NENHUM (não autentica). Um cookie vencido ou adulterado faria o próprio /auth/login
    //      responder 401, e a pessoa nunca mais conseguiria entrar. Essas rotas não precisam de login;
    //   b) o cookie "token" (o front Angular);
    //   c) o cabeçalho Authorization: Bearer <token> (Swagger, Postman, testes antigos).
    private BearerTokenResolver lerToken() {
        RequestMatcher rotasDeAutenticacao = PathPatternRequestMatcher.pathPattern("/auth/**");
        DefaultBearerTokenResolver lerDoCabecalho = new DefaultBearerTokenResolver();
        return request -> {
            if (rotasDeAutenticacao.matches(request)) {
                return null;
            }
            String doCookie = cookieAutenticacao.lerToken(request);
            return doCookie != null ? doCookie : lerDoCabecalho.resolve(request);
        };
    }

    // ---------------------------------------------------------------------------------------------
    // Respostas 401 e 403 (no mesmo formato ProblemDetail do GlobalExceptionHandler)
    // ---------------------------------------------------------------------------------------------

    // 401: sem token, token inválido, alterado, de outro emissor ou vencido. A mensagem é a MESMA em
    // todos os casos (não conta ao cliente QUAL foi o problema, para não ajudar quem está tentando forjar).
    // Se veio o cookie de login, ele não vale mais: a resposta já manda o "apagar", para o navegador
    // descartar um cookie velho ou adulterado sozinho.
    private AuthenticationEntryPoint respostaNaoAutenticado() {
        return (request, response, erro) -> {
            if (cookieAutenticacao.veioNaRequisicao(request)) {
                response.addHeader(HttpHeaders.SET_COOKIE, cookieAutenticacao.apagar().toString());
            }
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            escreverProblema(response, HttpStatus.UNAUTHORIZED, "Não autenticado", MENSAGEM_NAO_AUTENTICADO);
        };
    }

    // 403: tem token válido, mas não pode acessar esta rota
    private static AccessDeniedHandler respostaAcessoNegado() {
        return (request, response, erro) -> escreverProblema(response, HttpStatus.FORBIDDEN, "Acesso negado",
                "Você não tem permissão para acessar este recurso.");
    }

    // Monta o JSON à mão: os textos são fixos (nada vem da requisição), então não há risco de injeção.
    private static void escreverProblema(HttpServletResponse response, HttpStatus status, String titulo, String detalhe)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + titulo + "\",\"status\":"
                + status.value() + ",\"detail\":\"" + detalhe + "\"}");
    }

    // ---------------------------------------------------------------------------------------------
    // CORS
    // ---------------------------------------------------------------------------------------------

    // CORS = quais SITES podem chamar esta API pelo navegador. Só o front (Angular) é liberado.
    // Nunca "*": com credenciais ligadas, isso deixaria qualquer site do mundo chamar a API com o cookie
    // de quem estiver logado (o próprio Spring recusa "*" junto com allowCredentials, e o validarOrigem
    // abaixo impede a aplicação de subir com ele).
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origemPermitida));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        // o front consegue ler o Retry-After do 429 (o navegador esconde cabeçalhos não listados aqui)
        config.setExposedHeaders(List.of(HttpHeaders.RETRY_AFTER));
        // com credenciais: o navegador manda e recebe o cookie de login nas chamadas do front
        // (o Angular usa withCredentials: true). Sem isso, o navegador descartaria a resposta do login.
        config.setAllowCredentials(true);
        // o navegador guarda a resposta da "pergunta prévia" (preflight) por 1 hora
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", config);
        return fonte;
    }

    // Uma origem só, completa (ex.: "http://localhost:4200"). Recusa vazio, "*" e barra no final
    // (o navegador manda a origem sem barra, então "http://localhost:4200/" nunca bateria).
    private static String validarOrigem(String origem) {
        String valor = origem == null ? "" : origem.strip();
        boolean valida = (valor.startsWith("http://") || valor.startsWith("https://"))
                && !valor.contains("*") && !valor.contains(",") && !valor.endsWith("/");
        if (!valida) {
            throw new IllegalStateException("app.cors.origem-permitida (CORS_ORIGEM) inválida: use UMA origem "
                    + "completa, sem barra no final e sem '*', ex.: http://localhost:4200");
        }
        return valor;
    }
}
