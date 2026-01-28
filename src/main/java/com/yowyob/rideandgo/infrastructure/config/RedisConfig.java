package com.yowyob.rideandgo.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature; // Import
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
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
     * Crée un ObjectMapper customisé pour toute l'application (WebClient, Redis...).
     * Il est configuré pour gérer les dates/heures correctement.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        return new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(javaTimeModule)
                // Configuration cruciale :
                // Empêche Jackson d'écrire les dates comme des timestamps numériques (ex: 1674892800.12345)
                // et lui dit de les écrire en format String ISO-8601, ce qui est plus standard.
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }


    /**
     * Configure le template Redis pour utiliser notre ObjectMapper customisé.
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory, ObjectMapper objectMapper) { // Injection du bean ObjectMapper

        Jackson2JsonRedisSerializer<Object> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        RedisSerializationContext<String, Object> context =
                RedisSerializationContext.<String, Object>newSerializationContext(new StringRedisSerializer())
                        .value(jsonSerializer)
                        .hashValue(jsonSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}