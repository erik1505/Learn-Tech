package com.learntech.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReminderSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public ReminderSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS UK_Lembrete_Entrega_Aluno_Dia
                ON Lembrete_Entrega (Id_atividade, RA, Data_lembrete)
                """);
    }
}
