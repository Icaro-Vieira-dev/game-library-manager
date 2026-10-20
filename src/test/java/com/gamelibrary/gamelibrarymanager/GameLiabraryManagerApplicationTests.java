package com.gamelibrary.gamelibrarymanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Sobe a aplicação inteira para conferir que tudo se encaixa (controllers, services, repositórios, banco).
// O perfil "test" usa o banco H2 em memória (src/test/resources/application-test.properties),
// então este teste roda em qualquer máquina, inclusive no GitHub Actions, sem Postgres.
@SpringBootTest
@ActiveProfiles("test")
class GameLiabraryManagerApplicationTests {

    @Test
    void contextLoads() {
    }

}
