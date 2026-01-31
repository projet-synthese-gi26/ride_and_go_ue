package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.model.Wallet;
import com.yowyob.rideandgo.domain.ports.out.PaymentPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.PaymentApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentAdapter implements PaymentPort {
    private final PaymentApiClient client;

    @Override
    public Mono<Wallet> createWallet(UUID ownerId, String ownerName) {
        log.info("💳 Creating wallet for driver {} ({})", ownerName, ownerId);
        return client.createWallet(new PaymentApiClient.CreateWalletRequest(ownerId.toString(), ownerName))
                .map(this::mapToDomain);
    }

    @Override
    public Mono<Wallet> getWalletByOwnerId(UUID ownerId) {
        return client.getWalletByOwnerId(ownerId.toString())
                .map(this::mapToDomain)
                .doOnError(e -> log.error("❌ Failed to fetch wallet for owner {}: {}", ownerId, e.getMessage()));
    }

    @Override
    public Mono<Void> processPayment(UUID walletId, double offerPrice) {
        log.info("💸 Creating Payment Transaction for offer price: {} on wallet {}", offerPrice, walletId);
        // On envoie le PRIX DE L'OFFRE. Le service payment calculera le pourcentage à retirer.
        return client.createPaymentTransaction(new PaymentApiClient.PaymentTransactionRequest(
                walletId.toString(), offerPrice, "PAYMENT"));
    }

    private Wallet mapToDomain(PaymentApiClient.WalletResponse res) {
        return Wallet.builder()
                .id(UUID.fromString(res.id()))
                .ownerId(UUID.fromString(res.ownerId()))
                .ownerName(res.ownerName())
                .balance(res.balance())
                .build();
    }
}
