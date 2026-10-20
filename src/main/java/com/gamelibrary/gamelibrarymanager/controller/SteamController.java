package com.gamelibrary.gamelibrarymanager.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.gamelibrary.gamelibrarymanager.dto.steam.DetalhesSteamResultadoDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.ImportacaoSteamResultadoDTO;
import com.gamelibrary.gamelibrarymanager.service.SteamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Steam", description = "Importação da biblioteca e dos detalhes da loja da Steam")
@RestController
@RequestMapping("/steam")
public class SteamController {
    private final SteamService steamService;

    @Autowired
    public SteamController(SteamService steamService) {
        this.steamService = steamService;
    }

    /**
     * POST /steam/importacao
     * Importa a biblioteca da Steam.
     *   - Jogos novos são cadastrados (com o status calculado pelas horas jogadas).
     *   - Jogos que já existem têm SÓ as horas jogadas atualizadas; status, nota e o resto ficam como estão.
     * Pode chamar sempre que quiser sincronizar as horas.
     *
     * Antes de usar: preencha STEAM_API_KEY e STEAM_ID no arquivo .env (raiz do projeto) e reinicie a aplicação.
     *
     * Exemplos:
     * POST http://localhost:8080/steam/importacao                             -> usa o STEAM_ID do .env
     * POST http://localhost:8080/steam/importacao?steamId=76561198000000000    -> importa outro perfil
     *
     * Resposta: 200 com { jogosNovos, horasAtualizadas, novos: [lista dos jogos novos] }.
     * Erro 422: chave não configurada ou recusada, steamId inválido, ou perfil privado (a mensagem diz qual).
     */
    @PostMapping("/importacao")
    public ImportacaoSteamResultadoDTO importar(@RequestParam(required = false) String steamId) {
        return steamService.importarJogos(steamId);
    }

    /**
     * POST /steam/detalhes
     * Completa os jogos importados com dados da LOJA da Steam: gênero, categorias (uma por gênero),
     * desenvolvedora, ano de lançamento e plataforma (PC). Só preenche campos vazios.
     * Rode depois do POST /steam/importacao. Não precisa de API key.
     *
     * Processa até "limite" jogos por chamada (padrão 50, máximo 200), com 1,5 s entre cada um,
     * por causa do limite da loja. 50 jogos levam ~75 s. Se "restantes" > 0, chame de novo.
     *
     * Exemplos:
     *   POST http://localhost:8080/steam/detalhes
     *   POST http://localhost:8080/steam/detalhes?limite=10
     * Resposta: 200 com { processados, atualizados, semDadosNaLoja, restantes, mensagem }.
     */
    @PostMapping("/detalhes")
    public DetalhesSteamResultadoDTO completarDetalhes(@RequestParam(defaultValue = "50") int limite) {
        // garante o intervalo 1..200, para não travar a requisição por muito tempo
        int limiteSeguro = Math.max(1, Math.min(limite, 200));
        return steamService.completarDetalhes(limiteSeguro);
    }
}
