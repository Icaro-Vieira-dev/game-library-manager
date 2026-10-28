package com.gamelibrary.gamelibrarymanager.service;

import com.gamelibrary.gamelibrarymanager.exception.TentativasExcedidasException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

// Proteção contra força bruta no login: depois de 5 tentativas erradas em 15 minutos, aquela
// chave fica bloqueada por 15 minutos (429). Um login certo zera o contador. Os números vêm do
// application.properties (seguranca.login.*).
//
// A CHAVE é e-mail + IP de quem chamou. Por que não só o e-mail? Porque aí qualquer pessoa poderia
// bloquear a conta de OUTRA pessoa de propósito, errando a senha 5 vezes com o e-mail dela. Com o IP
// junto, o bloqueio só atinge quem está errando, daquele endereço.
//
// Fica em memória (ConcurrentHashMap): se a aplicação reiniciar, os contadores zeram; e com mais de
// uma instância da aplicação, cada uma conta separado (veja a lista de pendências no relatório).
@Component
public class LimiteTentativasLogin {

    // tentativas: quantas foram feitas na janela atual; inicioJanela: quando a 1ª delas aconteceu;
    // bloqueadoAte: null se não está bloqueado. Record imutável: cada mudança cria um novo.
    private record Registro(int tentativas, Instant inicioJanela, Instant bloqueadoAte) {
    }

    private final ConcurrentHashMap<String, Registro> registros = new ConcurrentHashMap<>();
    private final int maximoFalhas;
    private final Duration janela;
    private final Duration bloqueio;
    private final Clock relogio;
    // quando fazer a próxima faxina das entradas vencidas (para o mapa não crescer para sempre)
    private final AtomicReference<Instant> proximaLimpeza;

    public LimiteTentativasLogin(@Value("${seguranca.login.maximo-falhas:5}") int maximoFalhas,
                                 @Value("${seguranca.login.janela:PT15M}") Duration janela,
                                 @Value("${seguranca.login.bloqueio:PT15M}") Duration bloqueio,
                                 Clock relogio) {
        if (maximoFalhas < 1 || janela.isNegative() || janela.isZero() || bloqueio.isNegative() || bloqueio.isZero()) {
            throw new IllegalStateException("Configuração inválida em seguranca.login.*: maximo-falhas precisa ser "
                    + "pelo menos 1, e janela e bloqueio precisam ser maiores que zero.");
        }
        this.maximoFalhas = maximoFalhas;
        this.janela = janela;
        this.bloqueio = bloqueio;
        this.relogio = relogio;
        this.proximaLimpeza = new AtomicReference<>(relogio.instant().plus(janela));
    }

    // e-mail (já normalizado) + IP. O "|" separa os dois (não aparece em IP nem atrapalha o e-mail).
    public static String chave(String emailNormalizado, String ip) {
        return emailNormalizado + "|" + ip;
    }

    // Chamado ANTES de conferir a senha. Conta esta tentativa e lança 429 se passou do limite.
    //
    // Por que contar ANTES (e não só depois de errar)? Se contasse depois, 50 tentativas mandadas ao
    // mesmo tempo passariam todas pela verificação antes de qualquer uma registrar a falha, e o limite
    // de 5 seria furado. Aqui, verificar e contar é UMA operação atômica (compute), então isso não acontece.
    // O efeito para o usuário é o mesmo: 5 erros seguidos -> a próxima tentativa é bloqueada.
    public void consumirTentativa(String chave) {
        Instant agora = relogio.instant();
        limparVencidasSeForHora(agora);

        Registro registro = registros.compute(chave, (k, atual) -> {
            if (atual != null && atual.bloqueadoAte() != null) {
                if (agora.isBefore(atual.bloqueadoAte())) {
                    return atual; // continua bloqueado; esta tentativa nem conta
                }
                atual = null; // o bloqueio venceu: começa do zero
            }
            if (atual == null || janelaVenceu(atual, agora)) {
                return new Registro(1, agora, null);
            }
            int tentativas = atual.tentativas() + 1;
            // passou de 5 tentativas na janela (= 5 já falharam, porque o acerto zera): bloqueia
            Instant bloqueadoAte = tentativas > maximoFalhas ? agora.plus(bloqueio) : null;
            return new Registro(tentativas, atual.inicioJanela(), bloqueadoAte);
        });

        if (registro.bloqueadoAte() != null && agora.isBefore(registro.bloqueadoAte())) {
            throw new TentativasExcedidasException(segundosAte(agora, registro.bloqueadoAte()));
        }
    }

    // Login certo: zera o contador desta chave.
    public void registrarSucesso(String chave) {
        registros.remove(chave);
    }

    private boolean janelaVenceu(Registro registro, Instant agora) {
        return !agora.isBefore(registro.inicioJanela().plus(janela));
    }

    private boolean estaVencido(Registro registro, Instant agora) {
        if (registro.bloqueadoAte() != null) {
            return !agora.isBefore(registro.bloqueadoAte());
        }
        return janelaVenceu(registro, agora);
    }

    // No máximo uma faxina por janela (15 min). compareAndSet: se duas threads chegarem juntas,
    // só uma faz a faxina. O removeIf do ConcurrentHashMap só apaga a entrada se ela não mudou no meio.
    private void limparVencidasSeForHora(Instant agora) {
        Instant proxima = proximaLimpeza.get();
        if (agora.isBefore(proxima) || !proximaLimpeza.compareAndSet(proxima, agora.plus(janela))) {
            return;
        }
        registros.entrySet().removeIf(entrada -> estaVencido(entrada.getValue(), agora));
    }

    // arredonda para cima: 0,2 s restante vira 1 s (Retry-After: 0 faria o cliente tentar na hora)
    private static long segundosAte(Instant agora, Instant fim) {
        long milissegundos = Duration.between(agora, fim).toMillis();
        return Math.max(1, (milissegundos + 999) / 1000);
    }
}
