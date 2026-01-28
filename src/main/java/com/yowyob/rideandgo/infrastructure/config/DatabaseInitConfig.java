package com.yowyob.rideandgo.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.r2dbc.core.DatabaseClient;
import jakarta.annotation.PostConstruct;


@Configuration
@Profile("local") // Only active in local development
public class DatabaseInitConfig {

    private final DatabaseClient databaseClient;

    public DatabaseInitConfig(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @PostConstruct
    public void init() {

    }
}