package com.yowyob.rideandgo.infrastructure.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {
    // Utilisation d'une instance locale pour éviter les conflits avec la
    // configuration globale
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Simulation de tokens liés à des UUIDs réels de ta base (Mode Test/Swagger)
    private static final Map<String, String> STATIC_TOKENS = Map.of(
            "client-token", "7f13909e-7170-4f91-872e-333333333333",
            "driver-1-token", "a1b2c3d4-e5f6-4a5b-8c9d-111111111111",
            "driver-2-token", "a1b2c3d4-e5f6-4a5b-8c9d-222222222222");

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();

        // 1. Cas des tokens de test statiques (Swagger / Dev Local)
        if (STATIC_TOKENS.containsKey(authToken)) {
            String userId = STATIC_TOKENS.get(authToken);
            RoleType role = authToken.contains("driver")
                    ? RoleType.RIDE_AND_GO_DRIVER
                    : RoleType.RIDE_AND_GO_PASSENGER;

            return Mono.just(new UsernamePasswordAuthenticationToken(
                    userId,
                    authToken,
                    List.of(new SimpleGrantedAuthority(role.name()))));
        }

        // 2. Cas des vrais JWT (Remote / Production)
        if (authToken != null && !authToken.isEmpty()) {
            try {
                // Décodage basique du Payload (Partie 2 du JWT)
                String[] chunks = authToken.split("\\.");
                if (chunks.length < 2) {
                    return Mono.empty(); // Token malformé
                }

                Base64.Decoder decoder = Base64.getUrlDecoder();
                String payload = new String(decoder.decode(chunks[1]));

                // Parsing JSON
                Map<String, Object> claims = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
                });

                // Extraction de l'ID (Subject)
                String userId = (String) claims.get("sub");

                // Extraction des Rôles
                List<String> rolesClaim = (List<String>) claims.get("roles");
                List<SimpleGrantedAuthority> authorities;

                if (rolesClaim != null && !rolesClaim.isEmpty()) {
                    authorities = rolesClaim.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                } else {
                    authorities = List.of(new SimpleGrantedAuthority(RoleType.RIDE_AND_GO_PASSENGER.name()));
                }

                return Mono.just(new UsernamePasswordAuthenticationToken(
                        userId,
                        authToken,
                        authorities));

            } catch (Exception e) {
                log.error("Failed to decode JWT: {}", e.getMessage());
                return Mono.empty();
            }
        }

        return Mono.empty();
    }
}