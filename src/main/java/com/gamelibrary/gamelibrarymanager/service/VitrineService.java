package com.gamelibrary.gamelibrarymanager.service;

import com.gamelibrary.gamelibrarymanager.dto.JogoVitrineDTO;
import com.gamelibrary.gamelibrarymanager.dto.VitrineResponseDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamDestaquesResponseDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.SteamLojaAppDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

// Vitrine da loja da Steam: "Lançamentos" e "Em breve", sem jogos com conteúdo adulto.
//
// Como funciona:
//   - atualizar() roda sozinho, em segundo plano (@Scheduled), a cada 6 horas (configurável).
//     Ele consulta a loja, filtra e monta a vitrine pronta;
//   - buscar() (usado pelo GET /vitrine) só devolve a vitrine pronta, que fica em memória.
//     Assim a requisição do front é instantânea e nunca depende da Steam estar no ar.
// Não usa banco: se a aplicação reiniciar, a vitrine é montada de novo na primeira atualização.
@Service
public class VitrineService {
    private static final Logger log = LoggerFactory.getLogger(VitrineService.class);

    // seções da página inicial da loja (lançamentos, em breve, promoções...). Não precisa de API key.
    private static final String URL_DESTAQUES = "https://store.steampowered.com/api/featuredcategories";
    // detalhes de UM jogo (não aceita vários appids por consulta). Não precisa de API key.
    private static final String URL_LOJA_DETALHES = "https://store.steampowered.com/api/appdetails";
    private static final String URL_PAGINA_LOJA = "https://store.steampowered.com/app/";
    // ~200 consultas a cada 5 minutos = 1 a cada 1,5 s (a mesma pausa do SteamService)
    private static final Duration PAUSA_ENTRE_CONSULTAS_LOJA = Duration.ofMillis(1500);
    // no máximo 20 jogos por seção: 20 + 20 = 40 consultas ao appdetails = cerca de 1 minuto
    private static final int MAXIMO_POR_SECAO = 20;
    // no featuredcategories: 0 = jogo, 1 = pacote/bundle
    private static final int TIPO_JOGO = 0;

    // Descritores de conteúdo da Steam (appdetails -> content_descriptors.ids) que tiram o jogo da vitrine:
    //   3 = Adult Only Sexual Content
    //   4 = Frequent Nudity or Sexual Content
    // Os outros (1 = Some Nudity or Sexual Content, 2 = Frequent Violence or Gore, 5 = General Mature Content)
    // NÃO tiram: aparecem em muito jogo comum para maiores (ex.: jogos de tiro).
    // Atenção: o campo required_age NÃO serve para isso. O jogo adulto que testamos tinha required_age 0.
    static final Set<Integer> IDS_CONTEUDO_ADULTO = Set.of(3, 4);

    // o mesmo RestClient do resto da Steam (AppConfig), já com os tempos-limite configurados
    private final RestClient restClient;
    // relógio injetado (AppConfig) em vez de OffsetDateTime.now() direto: no teste dá para fixar a hora
    private final Clock relogio;

    // A vitrine pronta. AtomicReference porque duas threads mexem nela ao mesmo tempo:
    // a do agendamento escreve (atualizar) e as das requisições leem (buscar).
    // A troca é feita de uma vez só (set), então quem lê nunca vê uma vitrine pela metade.
    // Começa vazia, com atualizadoEm null (= ainda não foi carregada).
    private final AtomicReference<VitrineResponseDTO> vitrineAtual =
            new AtomicReference<>(new VitrineResponseDTO(List.of(), List.of(), null));

    @Autowired
    public VitrineService(RestClient restClient, Clock relogio) {
        this.restClient = restClient;
        this.relogio = relogio;
    }

    // Usado pelo GET /vitrine. Só devolve o que já está em memória: NUNCA chama a Steam.
    public VitrineResponseDTO buscar() {
        return vitrineAtual.get();
    }

    // Monta a vitrine de novo, consultando a loja da Steam. Roda em segundo plano:
    //   - a primeira vez, 10 s depois de a aplicação subir (vitrine.atraso-inicial);
    //   - depois, 6 h após o FIM da anterior (vitrine.intervalo-atualizacao). fixedDelay (e não fixedRate)
    //     garante que duas atualizações nunca rodam ao mesmo tempo.
    // Só fica ligado se o AgendamentoConfig estiver ativo (nos testes, não fica).
    //
    // Nenhuma exceção sai daqui: num @Scheduled ela só viraria um log de erro genérico do Spring, e a
    // vitrine ficaria desatualizada sem um aviso claro. Por isso tudo é tratado e logado aqui dentro.
    @Scheduled(initialDelayString = "${vitrine.atraso-inicial:PT10S}",
               fixedDelayString = "${vitrine.intervalo-atualizacao:PT6H}")
    public void atualizar() {
        log.info("Vitrine: atualização iniciada.");
        try {
            SteamDestaquesResponseDTO destaques;
            try {
                destaques = buscarDestaques();
            } catch (RestClientException e) {
                // erro HTTP, timeout, Steam fora do ar...: mantém a vitrine anterior (não apaga nada)
                log.warn("Vitrine: não foi possível buscar os destaques da loja da Steam. A vitrine anterior foi mantida.");
                return;
            }
            if (destaques == null) {
                log.warn("Vitrine: a loja da Steam respondeu sem conteúdo. A vitrine anterior foi mantida.");
                return;
            }

            // só jogos, sem repetidos, no máximo 20 por seção
            List<SteamDestaquesResponseDTO.Item> candidatosLancamentos =
                    selecionarJogos(itensDa(destaques.lancamentos()), MAXIMO_POR_SECAO);
            List<SteamDestaquesResponseDTO.Item> candidatosEmBreve =
                    selecionarJogos(itensDa(destaques.emBreve()), MAXIMO_POR_SECAO);

            // a mesma Verificacao nas duas seções: a pausa e a parada por 429 valem para as duas juntas
            Verificacao verificacao = new Verificacao();
            List<JogoVitrineDTO> lancamentos = verificarJogos(candidatosLancamentos, verificacao);
            List<JogoVitrineDTO> emBreve = verificarJogos(candidatosEmBreve, verificacao);

            // troca a vitrine inteira de uma vez
            vitrineAtual.set(new VitrineResponseDTO(lancamentos, emBreve, OffsetDateTime.now(relogio)));

            log.info("Vitrine: atualização concluída. Lançamentos: {}, em breve: {}, descartados como adultos: {}, "
                            + "descartados sem dados: {}{}",
                    lancamentos.size(), emBreve.size(), verificacao.adultos, verificacao.semDados,
                    verificacao.interrompida ? " (consultas interrompidas antes do fim)" : "");
        } catch (RuntimeException e) {
            // rede de segurança para qualquer erro que não previmos
            log.error("Vitrine: erro inesperado na atualização. A vitrine anterior foi mantida.", e);
        }
    }

    // Contadores de uma atualização (só existem durante um atualizar()).
    private static class Verificacao {
        int consultas;
        int adultos;
        int semDados;
        // true = a loja respondeu 429 (ou a thread foi interrompida): não consulta mais nada
        boolean interrompida;
    }

    // Consulta o appdetails de cada candidato e devolve só os aprovados, já no nosso formato.
    // Regra: NA DÚVIDA, FICA DE FORA. Se não der para confirmar que o jogo não é adulto, ele não entra.
    private List<JogoVitrineDTO> verificarJogos(List<SteamDestaquesResponseDTO.Item> candidatos, Verificacao verificacao) {
        List<JogoVitrineDTO> aprovados = new ArrayList<>();

        for (SteamDestaquesResponseDTO.Item item : candidatos) {
            if (verificacao.interrompida) {
                break;
            }
            // pausa entre as consultas (antes de todas, menos a primeira), por causa do limite da loja
            if (verificacao.consultas > 0 && !pausar(PAUSA_ENTRE_CONSULTAS_LOJA)) {
                verificacao.interrompida = true; // a aplicação está desligando: para aqui
                break;
            }
            verificacao.consultas++;

            SteamLojaAppDTO.Dados dados;
            try {
                dados = buscarDetalhes(item.id());
            } catch (HttpClientErrorException.TooManyRequests e) {
                // a loja pediu para esperar: para as consultas e fica só com os jogos já verificados
                log.warn("Vitrine: a loja da Steam limitou as consultas (429). A vitrine vai usar só os jogos verificados até aqui.");
                verificacao.interrompida = true;
                break;
            } catch (RestClientException e) {
                // erro ou timeout neste jogo: não dá para saber se é adulto, então fica de fora
                verificacao.semDados++;
                continue;
            }

            // sem "data", success=false ou sem a classificação de conteúdo: fica de fora
            if (dados == null || dados.descritores() == null) {
                verificacao.semDados++;
                continue;
            }
            // FILTRO DE ADULTO: é aqui que um jogo adulto é descartado
            if (ehConteudoAdulto(dados.descritores().ids())) {
                verificacao.adultos++;
                continue;
            }

            String dataLancamento = dados.dataLancamento() == null ? null : dados.dataLancamento().data();
            aprovados.add(paraJogoVitrine(item, dataLancamento));
        }
        return aprovados;
    }

    // GET featuredcategories. cc=br: preços em reais; l=brazilian: textos em português.
    private SteamDestaquesResponseDTO buscarDestaques() {
        URI url = UriComponentsBuilder.fromUriString(URL_DESTAQUES)
                .queryParam("cc", "br")
                .queryParam("l", "brazilian")
                .build()
                .toUri();
        return restClient.get().uri(url).retrieve().body(SteamDestaquesResponseDTO.class);
    }

    // GET appdetails de UM jogo. Devolve null se a loja não tiver os dados ({"success": false}, sem "data").
    // Mesmo formato usado no SteamService.buscarDadosLoja.
    private SteamLojaAppDTO.Dados buscarDetalhes(long appId) {
        URI url = UriComponentsBuilder.fromUriString(URL_LOJA_DETALHES)
                .queryParam("appids", appId)
                .queryParam("l", "brazilian")
                .build()
                .toUri();

        // a resposta é um mapa { "<appid>": { ... } }, por isso o ParameterizedTypeReference
        Map<String, SteamLojaAppDTO> resposta = restClient.get().uri(url).retrieve()
                .body(new ParameterizedTypeReference<Map<String, SteamLojaAppDTO>>() {});

        SteamLojaAppDTO app = (resposta == null) ? null : resposta.get(String.valueOf(appId));
        if (app == null || !app.success() || app.data() == null) {
            return null;
        }
        return app.data();
    }

    private static List<SteamDestaquesResponseDTO.Item> itensDa(SteamDestaquesResponseDTO.Secao secao) {
        return secao == null ? List.of() : secao.items();
    }

    // ---------------------------------------------------------------------------------------------
    // Métodos static: sem rede, sem banco e sem Spring. Dá para testar sozinhos
    // (como o SteamService.definirStatus).
    // ---------------------------------------------------------------------------------------------

    // Escolhe os candidatos de uma seção:
    //   - só jogos (type 0); pacotes/bundles (type 1) ficam de fora;
    //   - sem appId repetido: fica o PRIMEIRO (LinkedHashMap + putIfAbsent mantêm a ordem da loja);
    //   - no máximo "maximo" jogos.
    static List<SteamDestaquesResponseDTO.Item> selecionarJogos(List<SteamDestaquesResponseDTO.Item> itens, int maximo) {
        if (itens == null) {
            return List.of();
        }
        Map<Long, SteamDestaquesResponseDTO.Item> porAppId = new LinkedHashMap<>();
        for (SteamDestaquesResponseDTO.Item item : itens) {
            if (porAppId.size() >= maximo) {
                break;
            }
            if (item == null || item.id() == null || item.type() == null || item.type() != TIPO_JOGO) {
                continue;
            }
            porAppId.putIfAbsent(item.id(), item);
        }
        return List.copyOf(porAppId.values());
    }

    // true se a lista de descritores tiver algum dos IDS_CONTEUDO_ADULTO (3 ou 4).
    static boolean ehConteudoAdulto(List<Integer> idsDescritores) {
        if (idsDescritores == null) {
            return false;
        }
        for (Integer id : idsDescritores) {
            // id != null antes: Set.of(...).contains(null) lança NullPointerException
            if (id != null && IDS_CONTEUDO_ADULTO.contains(id)) {
                return true;
            }
        }
        return false;
    }

    // A Steam manda preço em CENTAVOS: 869 -> 8.69 (reais).
    // BigDecimal.valueOf(869, 2) = 869 com 2 casas decimais = 8.69, sem nenhum arredondamento.
    // null ou 0 -> null (no "Em breve" o preço vem 0, porque o jogo ainda não está à venda).
    static BigDecimal centavosParaReais(Integer centavos) {
        if (centavos == null || centavos == 0) {
            return null;
        }
        return BigDecimal.valueOf(centavos, 2);
    }

    // Converte um item da Steam (nomes em inglês) para o nosso formato.
    // dataLancamento vem do appdetails (o featuredcategories não traz a data).
    static JogoVitrineDTO paraJogoVitrine(SteamDestaquesResponseDTO.Item item, String dataLancamento) {
        return new JogoVitrineDTO(
                item.id(),
                item.name(),
                item.imagem(),
                centavosParaReais(item.precoFinal()),
                centavosParaReais(item.precoOriginal()),
                item.desconto(),
                dataLancamento,
                URL_PAGINA_LOJA + item.id());
    }

    // Espera a pausa entre consultas. Devolve false se a thread foi interrompida (a aplicação está
    // desligando): aí a atualização para, em vez de continuar consultando sem pausa.
    private static boolean pausar(Duration duracao) {
        try {
            Thread.sleep(duracao);
            return true;
        } catch (InterruptedException e) {
            // boa prática: se a thread foi interrompida, restaurar o aviso de interrupção
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
