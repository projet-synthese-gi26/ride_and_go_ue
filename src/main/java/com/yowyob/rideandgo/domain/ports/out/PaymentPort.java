package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.Wallet;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface PaymentPort {
    Mono<Wallet> createWallet(UUID ownerId, String ownerName);

    Mono<Wallet> getWalletByOwnerId(UUID ownerId);

    Mono<Void> processPayment(UUID walletId, double amount);
}