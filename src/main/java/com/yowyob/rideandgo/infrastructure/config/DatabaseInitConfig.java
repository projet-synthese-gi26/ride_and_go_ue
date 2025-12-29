package com.yowyob.rideandgo.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.core.DatabaseClient;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@Configuration
public class DatabaseInitConfig {

    private final DatabaseClient databaseClient;

    public DatabaseInitConfig(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @PostConstruct
    public void init() throws Exception {
        ClassPathResource resource = new ClassPathResource("schema.sql");
        String sql = Files.lines(Paths.get(resource.getURI()))
                          .collect(Collectors.joining("\n"));

        String[] statements = sql.split(";");

        Flux.fromArray(statements)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .flatMap(s -> databaseClient.sql(s).then())
            .subscribe(
                null,
                err -> System.err.println("Erreur lors de l'init DB : " + err),
                () -> System.out.println("Base initialisée avec succès")
            );
    }
}
