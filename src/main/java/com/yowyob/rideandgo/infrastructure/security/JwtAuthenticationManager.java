package com.yowyob.rideandgo.infrastructure.security;

import com.yowyob.rideandgo.domain.model.enums.RoleType; // <--- Import de tes vrais rôles
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    // Simulation de tokens liés à des UUIDs réels de ta base
    private static final Map<String, String> STATIC_TOKENS = Map.of(
        "client-token", "7f13909e-7170-4f91-872e-333333333333",
        "driver-1-token", "a1b2c3d4-e5f6-4a5b-8c9d-111111111111",
        "driver-2-token", "a1b2c3d4-e5f6-4a5b-8c9d-222222222222"
    );

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();
        
        // 1. Cas des tokens de test statiques (Swagger)
        if (STATIC_TOKENS.containsKey(authToken)) {
            String userId = STATIC_TOKENS.get(authToken);
            
            // Logique simple pour donner le bon rôle selon le nom du token de test
            RoleType role = authToken.contains("driver") 
                    ? RoleType.RIDE_AND_GO_DRIVER 
                    : RoleType.RIDE_AND_GO_PASSENGER;

            return Mono.just(new UsernamePasswordAuthenticationToken(
                    userId, 
                    authToken, // Token Relay activé
                    List.of(new SimpleGrantedAuthority(role.name())) // Utilisation du vrai Enum
            ));
        }

        // 2. Cas des vrais JWT (Remote / Production)
        if (authToken != null && !authToken.isEmpty()) {
            // NOTE : Dans un futur sprint, on décodera le JWT pour extraire les rôles "claims".
            // Pour l'instant, on assigne un rôle par défaut valide pour que Spring Security ne bloque pas.
            return Mono.just(new UsernamePasswordAuthenticationToken(
                    authToken, // Principal
                    authToken, // Token Relay activé
                    List.of(new SimpleGrantedAuthority(RoleType.RIDE_AND_GO_PASSENGER.name()))
            ));
        }
        
        return Mono.empty();
    }
}