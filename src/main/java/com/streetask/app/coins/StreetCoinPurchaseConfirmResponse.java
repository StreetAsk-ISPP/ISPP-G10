package com.streetask.app.coins;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StreetCoinPurchaseConfirmResponse {

    private UUID transactionId;
    private String status;
    private Integer addedStreetCoins;
    private Integer newBalance;
    private String externalPaymentId;
}
