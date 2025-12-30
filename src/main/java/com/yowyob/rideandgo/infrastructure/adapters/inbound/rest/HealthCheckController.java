package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "System diagnostic and connectivity checks")
public class HealthCheckController {

    private final DatabaseClient databaseClient;

    /**
     * Probes the database to ensure the R2DBC connection is active.
     */
    @GetMapping("/check-db")
    @Operation(summary = "Check database connectivity", description = "Executes a simple query to verify the DB link")
    public Mono<Map<String, Object>> checkDatabase() {
        return databaseClient.sql("SELECT 1")
                .map((row, metadata) -> row.get(0))
                .first()
                // Explicitly tell Java to treat this as a Map<String, Object>
                .map(res -> Map.<String, Object>of(
                        "status", "UP",
                        "database", "CONNECTED",
                        "message", "Database is responding correctly"
                ))
                .onErrorReturn(Map.of(
                        "status", "DOWN",
                        "database", "DISCONNECTED",
                        "message", "Could not connect to the database"
                ));
    }
}