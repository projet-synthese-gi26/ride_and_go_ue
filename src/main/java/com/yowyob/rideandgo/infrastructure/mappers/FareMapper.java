package com.yowyob.rideandgo.infrastructure.mappers;

import com.yowyob.rideandgo.domain.model.Fare;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FareMapper {

    /**
     * Mappe la réponse de l'API (Pynfi/Yowyob) vers le Domaine.
     */
    @Mapping(target = "estimatedFare", source = "prixMoyen") // Mapping du prix
    @Mapping(target = "officialFare", source = "prixMoyen")
    // Les champs suivants ne sont pas dans la réponse API racine, on les ignore ou on les laisse null
    @Mapping(target = "startPoint", ignore = true) 
    @Mapping(target = "endPoint", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "id", ignore = true)
    Fare toDomain(FareResponse response);

    /**
     * Mappe la requête entrante (DTO) vers le Domaine.
     */
    @Mapping(target = "startPoint", source = "depart") // Mapping français -> anglais
    @Mapping(target = "endPoint", source = "arrivee")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "estimatedFare", ignore = true)
    @Mapping(target = "officialFare", ignore = true)
    Fare toDomain(FareRequest request);
}