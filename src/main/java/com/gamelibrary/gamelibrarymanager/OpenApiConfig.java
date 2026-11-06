package com.gamelibrary.gamelibrarymanager;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Textos que aparecem no topo da documentação interativa (http://localhost:8080/swagger-ui.html).
// A lista de endpoints o springdoc monta sozinho, lendo os controllers.
@Configuration
public class OpenApiConfig {

    // O parâmetro "@UsuarioLogado Usuario usuario" dos controllers é preenchido pelo token, e não pelo
    // cliente. Sem isto, o Swagger o mostraria como um campo para o cliente preencher.
    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(UsuarioLogado.class);
    }

    @Bean
    public OpenAPI documentacaoDaApi() {
        return new OpenAPI().info(new Info()
                .title("Game Library Manager")
                .version("1.0")
                .description("API REST para gerenciar sua biblioteca de jogos com integração à Steam: "
                        + "importação da biblioteca, status automático, categorias por gênero, "
                        + "imagens e acompanhamento de conquistas/platina.")
                .contact(new Contact()
                        .name("Icaro da Silva")
                        .url("https://github.com/Icaro-Vieira-dev/game-library-manager")));
    }
}
