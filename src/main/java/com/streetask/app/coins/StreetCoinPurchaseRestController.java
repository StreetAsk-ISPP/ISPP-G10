package com.streetask.app.coins;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/streetcoins")
@SecurityRequirement(name = "bearerAuth")
public class StreetCoinPurchaseRestController {

    private final StreetCoinPurchaseService streetCoinPurchaseService;

    public StreetCoinPurchaseRestController(StreetCoinPurchaseService streetCoinPurchaseService) {
        this.streetCoinPurchaseService = streetCoinPurchaseService;
    }

    @GetMapping("/packs")
    public ResponseEntity<List<StreetCoinPackResponse>> getAvailablePacks() {
        return new ResponseEntity<>(streetCoinPurchaseService.getAvailablePacks(), HttpStatus.OK);
    }

    @GetMapping("/balance")
    public ResponseEntity<StreetCoinBalanceResponse> getCurrentBalance() {
        return new ResponseEntity<>(streetCoinPurchaseService.getCurrentBalance(), HttpStatus.OK);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<StreetCoinTransactionResponse>> getCurrentTransactions() {
        return new ResponseEntity<>(streetCoinPurchaseService.getCurrentTransactions(), HttpStatus.OK);
    }

    @PostMapping("/purchase")
    public ResponseEntity<StreetCoinPurchaseResponse> createPurchase(
            @Valid @RequestBody StreetCoinPurchaseRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        StreetCoinPurchaseResponse response = streetCoinPurchaseService.createPurchase(request, idempotencyKey);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/purchase/confirm")
    public ResponseEntity<StreetCoinPurchaseConfirmResponse> confirmPurchase(
            @Valid @RequestBody StreetCoinPurchaseConfirmRequest request) {
        StreetCoinPurchaseConfirmResponse response = streetCoinPurchaseService.confirmPurchase(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
