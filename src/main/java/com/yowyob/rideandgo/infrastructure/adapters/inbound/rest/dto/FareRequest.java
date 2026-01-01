package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

// Cette annotation retire les champs NULL du JSON envoyé
@JsonInclude(JsonInclude.Include.NON_NULL) 
public record FareRequest(
    @JsonProperty("depart") String depart,
    @JsonProperty("arrivee") String arrivee,
    @JsonProperty("heure") String heure,
    @JsonProperty("meteo") Integer meteo,
    @JsonProperty("type_zone") Integer typeZone,
    @JsonProperty("congestion_user") Integer congestionUser
) {}