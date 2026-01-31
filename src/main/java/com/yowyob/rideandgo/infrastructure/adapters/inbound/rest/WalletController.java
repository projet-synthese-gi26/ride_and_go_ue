package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.domain.model.Wallet;
import com.yowyob.rideandgo.domain.ports.out.PaymentPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Payment-Wallet", description = "Driver balance and transaction management")
public class WalletController {
    private final PaymentPort paymentPort;

    @GetMapping("/me")
    @Operation(summary = "Get My Wallet", description = "Retrieve balance for the current authenticated driver.")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_DRIVER')")
    public Mono<Wallet> getMyWallet() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> UUID.fromString(auth.getName()))
                .flatMap(paymentPort::getWalletByOwnerId);
    }
}