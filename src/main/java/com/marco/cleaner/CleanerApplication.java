package com.marco.cleaner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CleanerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleanerApplication.class, args);
    }

    @Bean
    public CommandLineRunner runAtStartup(CleanerService cleanerService) {
        return args -> {
            cleanerService.startCleaning();
        };
    }
}
