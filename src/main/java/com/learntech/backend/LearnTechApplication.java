package com.learntech.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import java.util.Arrays;

@SpringBootApplication
public class LearnTechApplication {
    public static void main(String[] args) {
        boolean importMode = Arrays.stream(args).anyMatch(argument -> argument.startsWith("--school.import.file="));
        SpringApplication application = new SpringApplication(LearnTechApplication.class);
        if (importMode) application.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext context = application.run(args);
        if (importMode) context.close();
    }
}