package com.yowyob.rideandgo.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.core.DatabaseClient;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Configuration
@Profile("local") // Only active in local development
public class DatabaseInitConfig {

    private final DatabaseClient databaseClient;

    public DatabaseInitConfig(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @PostConstruct
    public void init() {
        System.out.println("⚙️ LOCAL ENVIRONMENT: Starting sequential database initialization...");

        // 1. Run Schema -> 2. Check if users exist -> 3. Run Seeding if empty
        executeSqlFile("local/schema.sql")
                .then(checkIfDataExists())
                .flatMap(hasData -> {
                    if (!hasData) {
                        System.out.println("🌱 Database is empty. Injecting seed data...");
                        return executeSqlFile("local/data.sql");
                    }
                    System.out.println("✅ Database already has data. Skipping seeding.");
                    return Mono.empty();
                })
                .subscribe(
                        null,
                        err -> System.err.println("❌ Local DB Init Error: " + err.getMessage()),
                        () -> System.out.println("🚀 LOCAL DATABASE READY!")
                );
    }

    /**
     * Reads a SQL file and executes each statement sequentially using concatMap.
     */
    private Mono<Void> executeSqlFile(String filePath) {
        return Mono.fromCallable(() -> {
            ClassPathResource resource = new ClassPathResource(filePath);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        })
        .flatMapMany(sql -> Flux.fromArray(sql.split(";")))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .concatMap(s -> databaseClient.sql(s).then()) // Execute in strict sequence
        .then();
    }

    /**
     * Checks if the central 'users' table is populated.
     */
    private Mono<Boolean> checkIfDataExists() {
        return databaseClient.sql("SELECT COUNT(*) FROM users")
                .map((row, metadata) -> row.get(0, Long.class))
                .first()
                .map(count -> count > 0)
                .onErrorReturn(false); // If table doesn't exist, consider it empty
    }
}