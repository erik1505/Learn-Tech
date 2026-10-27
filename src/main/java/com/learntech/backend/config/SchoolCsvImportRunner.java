package com.learntech.backend.config;

import com.learntech.backend.service.SchoolCsvImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "school.import.file")
public class SchoolCsvImportRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(SchoolCsvImportRunner.class);
    private final SchoolCsvImportService importer;
    private final Environment environment;

    public SchoolCsvImportRunner(SchoolCsvImportService importer, Environment environment) {
        this.importer = importer;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        Path file = Path.of(environment.getRequiredProperty("school.import.file"));
        boolean dryRun = environment.getProperty("school.import.dry-run", Boolean.class, true);
        SchoolCsvImportService.ImportReport report = importer.importCsv(file, dryRun);
        if (report.dryRun()) {
            logger.info("Pré-validação concluída (nenhum dado foi alterado): {} linhas, {} escolas, {} turmas, até {} novas contas.",
                    report.rows(), report.schools(), report.classes(), report.createdAccounts());
        } else {
            logger.info("Importação concluída: {} linhas, {} escolas, {} turmas, {} novas contas e {} matrículas criadas.",
                    report.rows(), report.schools(), report.classes(), report.createdAccounts(), report.createdEnrollments());
        }
    }
}
