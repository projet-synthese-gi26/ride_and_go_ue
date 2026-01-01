package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * DTO complet aligné sur la réponse de l'API FareCalculator (Yowyob/Pynfi).
 */
public record FareResponse(
    @JsonProperty("statut") String statut,
    @JsonProperty("prix_moyen") Double prixMoyen,
    @JsonProperty("prix_min") Double prixMin,
    @JsonProperty("prix_max") Double prixMax,
    @JsonProperty("distance") Double distance,
    @JsonProperty("duree") Double duree,
    
    // Objets imbriqués complexes
    @JsonProperty("estimations_supplementaires") EstimationsSupplementaires estimationsSupplementaires,
    
    // Maps dynamiques pour les ajustements et détails (clé/valeur)
    @JsonProperty("ajustements_appliques") Map<String, Object> ajustementsAppliques,
    
    @JsonProperty("fiabilite") Integer fiabilite,
    @JsonProperty("message") String message,
    
    @JsonProperty("details_trajet") Map<String, Object> detailsTrajet,
    
    @JsonProperty("suggestions") List<String> suggestions
) {
    
    // --- Records internes pour la structure imbriquée ---

    public record EstimationsSupplementaires(
        @JsonProperty("ml_prediction") Double mlPrediction,
        @JsonProperty("features_utilisees") FeaturesUtilisees featuresUtilisees
    ) {}

    public record FeaturesUtilisees(
        @JsonProperty("distance_metres") Double distanceMetres,
        @JsonProperty("duree_secondes") Double dureeSecondes,
        @JsonProperty("congestion") Double congestion,
        @JsonProperty("sinuosite") Double sinuosite,
        @JsonProperty("nb_virages") Integer nbVirages,
        @JsonProperty("heure") String heure,
        @JsonProperty("meteo") Integer meteo,
        @JsonProperty("type_zone") Integer typeZone
    ) {}
}