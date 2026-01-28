package com.yowyob.rideandgo.infrastructure.config;

import com.yowyob.rideandgo.infrastructure.security.JwtAuthenticationManager;
import com.yowyob.rideandgo.infrastructure.security.SecurityContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {
        private final JwtAuthenticationManager authenticationManager;
        private final SecurityContextRepository securityContextRepository;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
                return http
                                .exceptionHandling(exceptionHandling -> exceptionHandling
                                                .authenticationEntryPoint((swe, e) -> Mono.fromRunnable(() -> swe
                                                                .getResponse().setStatusCode(HttpStatus.UNAUTHORIZED)))
                                                .accessDeniedHandler((swe,
                                                                e) -> Mono.fromRunnable(() -> swe.getResponse()
                                                                                .setStatusCode(HttpStatus.FORBIDDEN))))
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                                // --- CONFIGURATION CORS APPLIQUÉE ICI ---
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                // -----------------------------

                                .authenticationManager(authenticationManager)
                                .securityContextRepository(securityContextRepository)
                                .authorizeExchange(exchanges -> exchanges
                                                .pathMatchers(
                                                                "/api/v1/auth/**",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/webjars/**",
                                                                "/api/v1/health/**")
                                                .permitAll()
                                                // Autoriser les requêtes OPTIONS (Pre-flight CORS)
                                                .pathMatchers(org.springframework.http.HttpMethod.OPTIONS).permitAll()
                                                .anyExchange().authenticated())
                                .build();
        }

        // --- BEAN CORS CORRIGÉ POUR LA PROD ---
        @Bean
        CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // CORRECTION CRITIQUE :
                // Utiliser allowedOriginPatterns("*") au lieu de allowedOrigins("*")
                // permet d'utiliser allowCredentials(true) sans violer la spec CORS.
                configuration.setAllowedOriginPatterns(List.of("*"));

                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
                configuration.setAllowedHeaders(List.of("*"));

                // Indispensable pour que le frontend puisse envoyer le token Authorization
                configuration.setAllowCredentials(true);

                // Exposer les headers si besoin (ex: pagination)
                configuration.setExposedHeaders(List.of("*"));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}