package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.ports.out.RideRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.RideR2dbcRepository;
import com.yowyob.rideandgo.infrastructure.mappers.RideMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RideR2dbcAdapter implements RideRepositoryPort {
    private final RideR2dbcRepository repository;
    private final RideMapper mapper;

    @Override
    public Mono<Ride> save(Ride ride) {
        return repository.save(mapper.toEntity(ride))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Ride> findRideById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }
}
