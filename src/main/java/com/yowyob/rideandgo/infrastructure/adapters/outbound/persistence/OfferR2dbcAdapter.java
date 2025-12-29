package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.application.utils.Utils;
import com.yowyob.rideandgo.domain.model.Offer;
import com.yowyob.rideandgo.domain.ports.out.OfferRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.OfferAgreementEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.OfferEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.OfferAgreementR2dbcRepository;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.OfferR2dbcRepository;
import com.yowyob.rideandgo.infrastructure.mappers.OfferMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfferR2dbcAdapter implements OfferRepositoryPort {

    private final OfferAgreementR2dbcRepository offerAgreementRepository;
    private final OfferR2dbcRepository offerRepository;
    private final OfferMapper offerMapper;

    @Override
    @Transactional
    public Mono<Offer> save(Offer offer) {
        log.info("Saving offer: {}", offer);
        OfferEntity entity = offerMapper.toEntity(offer);

        return offerRepository.save(entity)
                .flatMap(savedOfferEntity -> {
                    if (offer.interestedDrivers() == null || offer.interestedDrivers().isEmpty()) {
                        return Mono.just(savedOfferEntity);
                    }

                    return Flux.fromIterable(offer.interestedDrivers())
                            .flatMap(driverId -> offerAgreementRepository
                                    .findByOfferIdAndDriverId(savedOfferEntity.getId(), driverId)
                                    .switchIfEmpty(Mono.defer(() -> {
                                        OfferAgreementEntity newAgreement = new OfferAgreementEntity(
                                                Utils.generateUUID(),
                                                driverId,
                                                savedOfferEntity.getId()
                                        );
                                        return offerAgreementRepository.save(newAgreement);
                                    })))
                            .collectList()
                            .map(agreements -> {
                                savedOfferEntity.setAgreements(agreements);
                                return savedOfferEntity;
                            });
                })
                .map(offerMapper::toDomain);
    }

    @Override
    public Mono<Boolean> delete(Offer offer) {
        return offerRepository.delete(offerMapper.toEntity(offer))
                .thenReturn(true)
                .onErrorReturn(false);
    }

    @Override
    public Mono<Boolean> exists(Offer offer) {
        return offerRepository.existsById(offer.id());
    }

    @Override
    public Mono<Offer> findById(UUID offerId) {
        return offerRepository.findById(offerId)
                .flatMap(entity -> {
                    return offerAgreementRepository.findByOfferId(offerId)
                            .collectList()
                            .map(agreements -> {
                                entity.setAgreements(agreements);
                                return entity;
                            });
                })
                .map(offerMapper::toDomain);
    }
}
