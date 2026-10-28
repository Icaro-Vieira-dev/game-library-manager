package com.gamelibrary.gamelibrarymanager.exception;

import com.gamelibrary.gamelibrarymanager.CookieAutenticacao;
import com.gamelibrary.gamelibrarymanager.SegurancaConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

// Todos os erros da API saem no mesmo formato, o ProblemDetail (padrão RFC 9457):
// {
//   "type": "about:blank",
//   "title": "Jogo não encontrado",          <- o tipo de problema, curto
//   "status": 404,                           <- o mesmo código HTTP da resposta
//   "detail": "Jogo do id: 99 Não encontrado", <- a explicação deste caso
//   "instance": "/jogos/99"                  <- a rota que deu erro
// }
// Erros de validação ganham também o campo "erros": { "campo": "mensagem" }.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final CookieAutenticacao cookieAutenticacao;

    public GlobalExceptionHandler(CookieAutenticacao cookieAutenticacao) {
        this.cookieAutenticacao = cookieAutenticacao;
    }

    @ExceptionHandler(JogoNaoEncontradoException.class)
    public ProblemDetail handleJogoNaoEncontrado(JogoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Jogo não encontrado", ex.getMessage());
    }

    @ExceptionHandler(CategoriaNaoEncontradaException.class)
    public ProblemDetail handleCategoriaNaoEncontrada(CategoriaNaoEncontradaException ex) {
        return problema(HttpStatus.NOT_FOUND, "Categoria não encontrada", ex.getMessage());
    }

    // @Valid no body: um item em "erros" para cada campo inválido
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            // isBindingFailure = o valor nem pôde ser convertido (ex.: ?status=XPTO, ?anoLancamento=abc)
            String mensagem = erro.isBindingFailure()
                    ? "Valor inválido: '" + erro.getRejectedValue() + "'."
                    : erro.getDefaultMessage();
            erros.putIfAbsent(erro.getField(), mensagem);
        }
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos estão inválidos. Veja o campo \"erros\".");
        problema.setProperty("erros", erros);
        return problema;
    }

    // ex.: GET /jogos/abc cai em /jogos/{id} e "abc" não é um número
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "Valor inválido para '" + ex.getName() + "': " + ex.getValue() + ". Verifique se a rota está correta.");
    }

    // JSON malformado, ou um valor que não cabe no campo (ex.: "statusJogo": "ZERADO")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleJsonInvalido(HttpMessageNotReadableException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido",
                "O JSON enviado está malformado ou tem um campo com valor inválido "
                        + "(ex.: statusJogo aceita JOGANDO, FINALIZADO, ABANDONADO ou PLANEJADO).");
    }

    // ?sort=campoQueNaoExiste
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleOrdenacaoInvalida(PropertyReferenceException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Ordenação inválida",
                "Não dá para ordenar por '" + ex.getPropertyName() + "'. Use um campo do jogo, ex.: sort=nomeJogo ou sort=horasJogadas,desc.");
    }

    @ExceptionHandler(SteamImportacaoException.class)
    public ProblemDetail handleSteamImportacao(SteamImportacaoException ex) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Não foi possível falar com a Steam", ex.getMessage());
    }

    // um serviço externo (Steam) não respondeu dentro do tempo-limite (AppConfig), ou a internet caiu.
    // Não use ex.getMessage() aqui nem no log: ela contém a URL da requisição, e a da API da Steam leva a chave.
    @ExceptionHandler(ResourceAccessException.class)
    public ProblemDetail handleSemResposta(ResourceAccessException ex) {
        return problema(HttpStatus.GATEWAY_TIMEOUT, "O serviço externo não respondeu",
                "O serviço externo (Steam) demorou demais para responder ou está fora do ar. Tente de novo em alguns minutos.");
    }

    // 409 Conflict: o pedido é válido, mas conflita com o estado atual (a categoria ainda tem jogos)
    @ExceptionHandler(CategoriaEmUsoException.class)
    public ProblemDetail handleCategoriaEmUso(CategoriaEmUsoException ex) {
        return problema(HttpStatus.CONFLICT, "Categoria em uso", ex.getMessage());
    }

    // 409 também: criar uma categoria que já existe conflita com o que está no banco
    @ExceptionHandler(CategoriaDuplicadaException.class)
    public ProblemDetail handleCategoriaDuplicada(CategoriaDuplicadaException ex) {
        return problema(HttpStatus.CONFLICT, "Categoria já existe", ex.getMessage());
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ProblemDetail handleRequisicaoInvalida(RequisicaoInvalidaException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage());
    }

    // ===== Autenticação =====
    // Regra destes handlers: nunca colocar senha, hash, token ou chave na resposta nem no log.
    // As mensagens das nossas exceções são textos fixos (veja cada classe).

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ProblemDetail handleEmailJaCadastrado(EmailJaCadastradoException ex) {
        return problema(HttpStatus.CONFLICT, "E-mail já cadastrado", ex.getMessage());
    }

    @ExceptionHandler(SteamIdJaVinculadoException.class)
    public ProblemDetail handleSteamIdJaVinculado(SteamIdJaVinculadoException ex) {
        return problema(HttpStatus.CONFLICT, "Steam ID já vinculado", ex.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail handleCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "Não autenticado", ex.getMessage());
    }

    // 429 + Retry-After: o cabeçalho diz em quantos segundos o cliente pode tentar de novo
    @ExceptionHandler(TentativasExcedidasException.class)
    public ResponseEntity<ProblemDetail> handleTentativasExcedidas(TentativasExcedidasException ex) {
        ProblemDetail problema = problema(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosParaTentarDeNovo()))
                .body(problema);
    }

    // Erros do Spring Security que acontecem DENTRO de um controller (ex.: token de um usuário que não
    // existe mais, no GET /usuarios/me). Sem estes dois handlers, o handler genérico de Exception logo
    // abaixo os pegaria e responderia 500. A mensagem é fixa: não repete o motivo técnico.
    // (Os erros de token que acontecem ANTES do controller são respondidos pelo SegurancaConfig,
    // com os mesmos textos e o mesmo cuidado com o cookie.)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleNaoAutenticado(AuthenticationException ex, HttpServletRequest request) {
        ProblemDetail problema = problema(HttpStatus.UNAUTHORIZED, "Não autenticado",
                SegurancaConfig.MENSAGEM_NAO_AUTENTICADO);
        ResponseEntity.BodyBuilder resposta = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        // o cookie de login não vale mais (ex.: a conta foi apagada): manda o navegador descartá-lo
        if (cookieAutenticacao.veioNaRequisicao(request)) {
            resposta.header(HttpHeaders.SET_COOKIE, cookieAutenticacao.apagar().toString());
        }
        return resposta.body(problema);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAcessoNegado(AccessDeniedException ex) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado", "Você não tem permissão para acessar este recurso.");
    }

    // Rede de segurança: qualquer outro erro.
    // - Erros do próprio Spring (rota inexistente = 404, método errado = 405...) já sabem o seu
    //   ProblemDetail (implementam ErrorResponse): devolvemos o deles, com o código certo.
    // - O resto é um erro inesperado nosso: 500, com uma mensagem genérica. O detalhe técnico vai
    //   para o LOG do servidor, e não para a resposta (não se expõe stack trace para o cliente).
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleErroInesperado(Exception ex) {
        if (ex instanceof ErrorResponse erroDoSpring) {
            return erroDoSpring.getBody();
        }
        log.error("Erro inesperado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente; se continuar, verifique o log do servidor.");
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
