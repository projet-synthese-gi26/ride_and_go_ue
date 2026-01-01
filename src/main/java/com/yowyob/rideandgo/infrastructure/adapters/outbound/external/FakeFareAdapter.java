package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.ports.out.FareClientPort;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class FakeFareAdapter implements FareClientPort {

    @Override
    public Mono<FareResponse> caclculateFare(FareRequest request) {
        log.info("🛠 MODE FAKE FARE : Calcul pour {} -> {}", request.depart(), request.arrivee());
        
        double randomPrice = ThreadLocalRandom.current().nextDouble(1500.0, 5000.0);
        double roundedPrice = Math.round(randomPrice / 100.0) * 100.0; // Arrondi à 100

        // Construction des sous-objets Fake
        FareResponse.FeaturesUtilisees features = new FareResponse.FeaturesUtilisees(
            5500.0, 900.0, 5.0, 1.2, 12, "matin", 1, 2
        );
        
        FareResponse.EstimationsSupplementaires estimations = new FareResponse.EstimationsSupplementaires(
            roundedPrice, features
        );

        return Mono.just(new FareResponse(
                "exact",
                roundedPrice,       // prix_moyen
                roundedPrice - 200, // prix_min
                roundedPrice + 200, // prix_max
                5.5,                // distance
                15.0,               // duree
                estimations,        // estimations_supplementaires
                Map.of("Heure de pointe", "+500F"), // ajustements_appliques
                1,                  // fiabilite
                "Estimation Fake réussie",
                Map.of("type_route", "bitume"),     // details_trajet
                List.of("Eviter le centre ville")   // suggestions
        ));
    }
}