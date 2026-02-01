package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.model.enums.RideState;
import com.yowyob.rideandgo.domain.ports.out.RideRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.RideEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.RideR2dbcRepository;
import com.yowyob.rideandgo.infrastructure.mappers.RideMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RideR2dbcAdapter implements RideRepositoryPort {
    private final RideR2dbcRepository repository;
    private final RideMapper mapper;

    @Override
    @Transactional
    public Mono<Ride> save(Ride ride) {
        RideEntity entity = mapper.toEntity(ride);
        entity.setNewEntity(ride.state() == RideState.CREATED && ride.timeReal() == 0);

        return repository.save(entity)
                .map(mapper::toDomain)
                .switchIfEmpty(Mono.just(ride));
    }

    @Override
    public Mono<Ride> findRideById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Mono<Ride> findCurrentRideByDriverId(UUID driverId) {
        return repository.findActiveRideByDriverId(driverId).map(mapper::toDomain);
    }

    @Override
    public Mono<Ride> findRideByOfferId(UUID offerId) {
        return repository.findByOfferId(offerId).map(mapper::toDomain);
    }

    @Override
    public Mono<Long> countCompletedRidesByDriverId(UUID driverId) {
        return repository.countCompletedByDriverId(driverId);
    }

    @Override
    public Flux<Ride> findRideHistoryByUserId(UUID userId, int page, int size) {
        long offset = (long) page * size;
        return repository.findHistoryByUserId(userId, offset, size).map(mapper::toDomain);
    }

    @Override
    public Flux<Ride> findRideHistoryByDriverId(UUID driverId, int page, int size) {
        long offset = (long) page * size;
        return repository.findHistoryByDriverId(driverId, offset, size).map(mapper::toDomain);
    }
}
