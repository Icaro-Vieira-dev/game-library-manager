package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.dto.VitrineResponseDTO;
import com.gamelibrary.gamelibrarymanager.service.VitrineService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Vitrine", description = "Lançamentos e jogos em breve da loja da Steam")
@RestController
@RequestMapping("/vitrine")
public class VitrineController {
    // quanto tempo o navegador (ou um proxy) pode reaproveitar a vitrine sem perguntar de novo.
    // A vitrine muda no máximo a cada 6 horas, então 5 minutos de atraso não fazem diferença.
    private static final Duration TEMPO_DE_CACHE = Duration.ofMinutes(5);

    private final VitrineService vitrineService;

    @Autowired
    public VitrineController(VitrineService vitrineService) {
        this.vitrineService = vitrineService;
    }

    /**
     * GET /vitrine
     * Vitrine da loja da Steam: "Lançamentos" e "Em breve" (até 20 jogos em cada), com preço em reais
     * e textos em português. Jogos com conteúdo adulto ficam de fora.
     * Os dados vêm da loja da Steam, mas são atualizados em segundo plano a cada 6 horas
     * (vitrine.intervalo-atualizacao): esta rota só devolve a vitrine que já está pronta em memória,
     * por isso é rápida e funciona mesmo se a Steam estiver fora do ar.
     *
     * CACHE: é a ÚNICA rota com cache liberado (Cache-Control: max-age=300, public). Pode porque a vitrine
     * é pública e igual para todos (não depende de quem está logado). Por isso o atualizadoEm que o front
     * mostra pode estar até 5 minutos "atrasado". Todas as outras rotas continuam com o no-store padrão
     * do Spring Security, porque podem ter dados de um usuário (/jogos vai ser de cada usuário em breve).
     *
     * Exemplo: GET http://localhost:8080/vitrine
     * Resposta: 200 sempre, com { lancamentos: [...], emBreve: [...], atualizadoEm }
     *           e o cabeçalho Cache-Control: max-age=300, public.
     *           Logo depois de a aplicação subir, as listas podem vir vazias com atualizadoEm null
     *           (a primeira carga leva cerca de 1 minuto).
     */
    @GetMapping
    public ResponseEntity<VitrineResponseDTO> buscar() {
        // O Spring Security não sobrescreve este cabeçalho: ele só escreve o seu no-store quando a
        // resposta ainda NÃO tem Cache-Control (nem Expires ou Pragma), e só no fim, depois do controller.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(TEMPO_DE_CACHE).cachePublic())
                .body(vitrineService.buscar());
    }
}
