package com.yowyob.rideandgo.infrastructure.config;

import com.yowyob.rideandgo.domain.model.enums.OfferState;
import com.yowyob.rideandgo.domain.model.enums.RideState;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

import java.util.Arrays;

/**
 * Configuration R2DBC avec gestion des ENUMs PostgreSQL via Converters Spring Data.
 * Cette approche est plus fiable que EnumCodec car elle utilise CAST SQL explicites.
 */
@Configuration
public class R2dbcConfig {

    /**
     * Enregistre les converters personnalisés pour les ENUMs PostgreSQL.
     */
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, Arrays.asList(
                // OfferState converters
                new OfferStateWritingConverter(),
                new OfferStateReadingConverter(),
                // RideState converters
                new RideStateWritingConverter(),
                new RideStateReadingConverter()
        ));
    }

    /**
     * Active la gestion transactionnelle réactive
     */
    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    // ========== OFFER STATE CONVERTERS ==========

    /**
     * Convertit l'enum Java OfferState vers String pour PostgreSQL.
     * PostgreSQL cast automatiquement "STRING"::offer_state_enum
     */
    @WritingConverter
    static class OfferStateWritingConverter implements Converter<OfferState, String> {
        @Override
        public String convert(OfferState source) {
            return source.name();
        }
    }

    /**
     * Convertit la String PostgreSQL vers l'enum Java OfferState
     */
    @ReadingConverter
    static class OfferStateReadingConverter implements Converter<String, OfferState> {
        @Override
        public OfferState convert(String source) {
            return OfferState.valueOf(source);
        }
    }

    // ========== RIDE STATE CONVERTERS ==========

    @WritingConverter
    static class RideStateWritingConverter implements Converter<RideState, String> {
        @Override
        public String convert(RideState source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class RideStateReadingConverter implements Converter<String, RideState> {
        @Override
        public RideState convert(String source) {
            return RideState.valueOf(source);
        }
    }
}