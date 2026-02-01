package com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

@HttpExchange("/api/v1")
public interface PaymentApiClient {
    
    @PostExchange("/wallets")
    Mono<WalletResponse> createWallet(@RequestBody CreateWalletRequest request);

    @GetExchange("/wallets/owner/{id}")
    Mono<WalletResponse> getWalletByOwnerId(@PathVariable("id") String ownerId);

    @PostExchange("/transactions/payment")
    Mono<Void> createPaymentTransaction(@RequestBody PaymentTransactionRequest request);
    
    record CreateWalletRequest(String ownerId, String ownerName) {
    }

    record PaymentTransactionRequest(String walletId, double amount, String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WalletResponse(String id, String ownerId, String ownerName, double balance) {
    }
}