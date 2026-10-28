package com.gamelibrary.gamelibrarymanager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class AppConfig {

    // Cliente HTTP usado em todas as chamadas à Steam: a API oficial (importação e conquistas, no
    // SteamService) e a loja (detalhes dos jogos no SteamService e a vitrine no VitrineService).
    // O RestClient substitui o RestTemplate (que está em modo de manutenção desde o Spring 6.1)
    // e tem uma escrita "encadeada":
    //   restClient.get().uri(url).retrieve().body(Classe.class)
    @Bean
    public RestClient restClient() {
        // tempos-limite: sem eles, uma Steam lenta deixaria a requisição esperando para sempre
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));   // para conseguir conectar
        fabrica.setReadTimeout(Duration.ofSeconds(20));     // para a resposta chegar

        return RestClient.builder()
                .requestFactory(fabrica)
                .build();
    }

    // O "relógio" da aplicação, em UTC. Quem precisa da hora atual recebe este bean em vez de chamar
    // OffsetDateTime.now() direto: assim um teste pode trocar por um Clock.fixed(...) e fixar a hora.
    @Bean
    public Clock relogio() {
        return Clock.systemUTC();
    }
}
