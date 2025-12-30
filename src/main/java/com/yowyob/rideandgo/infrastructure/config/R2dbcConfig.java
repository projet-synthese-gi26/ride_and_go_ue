package com.yowyob.rideandgo.infrastructure.config;

import com.yowyob.rideandgo.domain.model.enums.OfferState;
import com.yowyob.rideandgo.domain.model.enums.RideState;
import io.r2dbc.postgresql.codec.EnumCodec;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class R2dbcConfig {

    /**
     * Configures the PostgreSQL driver to recognize custom ENUM types.
     * The ConnectionFactoryCustomizer comes from Spring Boot Autoconfigure.
     */
    @Bean
    public ConnectionFactoryCustomizer postgresCustomizer() {
        return (builder) -> {
            builder.codecRegistrar(EnumCodec.builder()
                    // Link Java OfferState to Postgres type 'offer_state_enum'
                    .withEnum("offer_state_enum", OfferState.class)
                    // Link Java RideState to Postgres type 'ride_state_enum'
                    .withEnum("ride_state_enum", RideState.class)
                    .build());
        };
    }
}