package com.streetask.app.coins;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StreetCoinPurchaseResponse {

    private UUID transactionId;
    private String status;
    private Integer streetCoins;
    private String currency;
    private String idempotencyKey;
    private String sessionId;
    private String checkoutUrl;
    private String publishableKey;
}
