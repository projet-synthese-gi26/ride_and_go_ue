package com.yowyob.rideandgo.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Configures the security filter chain for WebFlux.
     * Defines which paths are public and which are secured.
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        System.out.println("🛡️ REACTIVE SECURITY CONFIGURATION LOADED");

        return http
                // Disable CSRF for stateless REST APIs
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints: Monitoring and Health
                        .pathMatchers("/actuator/**", "/api/v1/health/**").permitAll()
                        
                        // Public endpoints: API Documentation (Swagger)
                        .pathMatchers(
                                "/v3/api-docs/**", 
                                "/swagger-ui/**", 
                                "/swagger-ui.html", 
                                "/webjars/**"
                        ).permitAll()
                        
                        // All other business endpoints require authentication
                        .anyExchange().authenticated()
                )
                
                // Disable default login mechanisms
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }

    /**
     * Password encoder used for user credential hashing.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Dummy user details service to prevent Spring from logging 
     * a generated security password on every startup.
     */
    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails dummy = User.withUsername("tech_user")
                .password("{noop}secret")
                .roles("SYSTEM")
                .build();
        return new MapReactiveUserDetailsService(dummy);
    }
}