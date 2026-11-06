package com.gamelibrary.gamelibrarymanager.controller;

import com.gamelibrary.gamelibrarymanager.UsuarioLogado;
import com.gamelibrary.gamelibrarymanager.model.Usuario;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.gamelibrary.gamelibrarymanager.dto.steam.DetalhesSteamResultadoDTO;
import com.gamelibrary.gamelibrarymanager.dto.steam.ImportacaoSteamResultadoDTO;
import com.gamelibrary.gamelibrarymanager.service.SteamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

// TODAS as rotas daqui exigem login e mexem só na biblioteca do USUÁRIO LOGADO.
// Sem login, ou com token inválido: 401 em todas.
@Tag(name = "Steam", description = "Importação da biblioteca e dos detalhes da loja da Steam para o usuário logado (exige login)")
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
     * Importa a biblioteca da Steam do USUÁRIO LOGADO, usando o Steam ID cadastrado nele
     * (no cadastro ou no PATCH /usuarios/me). Exige login.
     *   - Jogos novos são cadastrados para você (com o status calculado pelas horas jogadas).
     *   - Jogos que você já tem têm SÓ as horas jogadas atualizadas; status, nota e o resto ficam como estão.
     *   - Os jogos das outras pessoas não são tocados (duas pessoas podem ter o mesmo jogo).
     * Pode chamar sempre que quiser sincronizar as horas.
     * A chave da API da Steam é a do servidor (STEAM_API_KEY): nunca pedimos a sua.
     *
     * Exemplo: POST http://localhost:8080/steam/importacao   (sem body)
     *
     * Resposta: 200 com { jogosNovos, horasAtualizadas, novos: [lista dos jogos novos] }. 401 sem login.
     * Erro 422: "Cadastre o seu Steam ID antes de importar (PATCH /usuarios/me).", chave do servidor não
     *           configurada ou recusada, ou perfil privado (a mensagem diz qual).
     */
    @PostMapping("/importacao")
    public ImportacaoSteamResultadoDTO importar(@UsuarioLogado Usuario usuario) {
        return steamService.importarJogos(usuario);
    }

    /**
     * POST /steam/detalhes
     * Completa os jogos da Steam DO USUÁRIO LOGADO com dados da LOJA da Steam: gênero, categorias (uma por
     * gênero, criadas para você), desenvolvedora, ano de lançamento e plataforma (PC). Só preenche campos vazios.
     * Rode depois do POST /steam/importacao. Exige login (a loja não precisa de API key).
     *
     * Processa até "limite" jogos por chamada (padrão 50, máximo 200), com 1,5 s entre cada um,
     * por causa do limite da loja. 50 jogos levam ~75 s. Se "restantes" > 0, chame de novo.
     *
     * Exemplos:
     *   POST http://localhost:8080/steam/detalhes
     *   POST http://localhost:8080/steam/detalhes?limite=10
     * Resposta: 200 com { processados, atualizados, semDadosNaLoja, restantes, mensagem }. 401 sem login.
     */
    @PostMapping("/detalhes")
    public DetalhesSteamResultadoDTO completarDetalhes(@RequestParam(defaultValue = "50") int limite,
                                                       @UsuarioLogado Usuario usuario) {
        // garante o intervalo 1..200, para não travar a requisição por muito tempo
        int limiteSeguro = Math.max(1, Math.min(limite, 200));
        return steamService.completarDetalhes(limiteSeguro, usuario);
    }
}
