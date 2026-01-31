package com.yowyob.rideandgo.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
        /**
         * ObjectMapper standard pour l'application (WebClient, Controllers, etc.).
         * N'active PAS le default typing. C'est le bean par défaut.
         */
        @Bean
        @Primary
        public ObjectMapper objectMapper() {
                JavaTimeModule javaTimeModule = new JavaTimeModule();

                return new ObjectMapper()
                                .registerModule(new ParameterNamesModule())
                                .registerModule(javaTimeModule)
                                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        }

        /**
         * ObjectMapper SPÉCIFIQUE et ISOLÉ pour Redis.
         * Il est construit à partir de zéro pour ne pas muter le bean primaire.
         * Il active le "default typing" pour la désérialisation polymorphique.
         */
        @Bean
        @Qualifier("redisObjectMapper")
        public ObjectMapper redisObjectMapper() {
                JavaTimeModule javaTimeModule = new JavaTimeModule();

                ObjectMapper objectMapper = new ObjectMapper()
                                .registerModule(new ParameterNamesModule())
                                .registerModule(javaTimeModule)
                                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

                // Activation du "Typing" uniquement pour cette instance d'ObjectMapper.
                objectMapper.activateDefaultTyping(
                                LaissezFaireSubTypeValidator.instance,
                                ObjectMapper.DefaultTyping.NON_FINAL);

                return objectMapper;
        }

        /**
         * Configure le template Redis pour utiliser l'ObjectMapper SPÉCIFIQUE à Redis.
         */
        @Bean
        public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
                        ReactiveRedisConnectionFactory factory,
                        @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) { // Injection du bean qualifié

                Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(
                                redisObjectMapper, Object.class);

                RedisSerializationContext<String, Object> context = RedisSerializationContext
                                .<String, Object>newSerializationContext(new StringRedisSerializer())
                                .value(jsonSerializer)
                                .hashValue(jsonSerializer)
                                .build();

                return new ReactiveRedisTemplate<>(factory, context);
        }
}