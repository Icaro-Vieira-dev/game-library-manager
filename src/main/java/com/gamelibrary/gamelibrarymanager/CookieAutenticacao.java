package com.gamelibrary.gamelibrarymanager;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// O ÚNICO lugar que sabe como é o cookie de login: nome e atributos.
// O navegador só apaga um cookie se o "apagar" vier com o MESMO nome, path e atributos do original;
// por isso os dois são montados aqui, a partir da mesma base (cookieBase).
//
// Atributos:
//   - HttpOnly: o JavaScript da página NÃO consegue ler o cookie. Se o front sofrer um XSS
//     (script malicioso injetado), o token não pode ser roubado;
//   - Secure: o navegador só manda o cookie por HTTPS (http://localhost é aceito como exceção);
//   - SameSite=Strict: o navegador não manda o cookie quando a requisição começa em OUTRO site
//     (primeira barreira contra CSRF; a segunda é o FiltroOrigem);
//   - Path=/: vale para a API inteira;
//   - sem Domain: o cookie fica preso ao host exato da API (não vaza para subdomínios).
@Component
public class CookieAutenticacao {

    public static final String NOME = "token";

    private final boolean secure;

    public CookieAutenticacao(@Value("${app.cookie.secure}") boolean secure) {
        this.secure = secure;
    }

    // Cookie do login: leva o JWT e vence junto com ele.
    public ResponseCookie criar(String token, Duration validade) {
        return cookieBase(token).maxAge(validade).build();
    }

    // Cookie de "apagar": mesmo nome e atributos, valor vazio e Max-Age=0 (o navegador descarta na hora).
    public ResponseCookie apagar() {
        return cookieBase("").maxAge(Duration.ZERO).build();
    }

    // O token do cookie, ou null se o cookie não veio ou veio vazio.
    public String lerToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (NOME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    // true se a requisição trouxe o cookie de login preenchido (usado no 401 para mandar o "apagar")
    public boolean veioNaRequisicao(HttpServletRequest request) {
        return lerToken(request) != null;
    }

    private ResponseCookie.ResponseCookieBuilder cookieBase(String valor) {
        return ResponseCookie.from(NOME, valor)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/");
    }
}
