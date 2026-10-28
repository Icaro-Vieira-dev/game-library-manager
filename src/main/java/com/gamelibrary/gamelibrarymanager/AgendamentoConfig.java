package com.gamelibrary.gamelibrarymanager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Liga as tarefas agendadas (@Scheduled), como a atualização da vitrine (VitrineService.atualizar).
// Sem o @EnableScheduling, o Spring ignora os @Scheduled: o método existe, mas nunca roda sozinho.
//
// Só liga se vitrine.atualizacao-ativa for true (ou se a propriedade nem existir: matchIfMissing).
// Nos testes ela é false (application-test.properties): os testes nunca chamam a Steam de verdade.
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "vitrine.atualizacao-ativa", havingValue = "true", matchIfMissing = true)
public class AgendamentoConfig {
}
