package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

public record UpdateOfferRequest(
    String startPoint,
    String endPoint,
    Double price
) {}