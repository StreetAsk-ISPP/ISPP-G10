package com.streetask.app.coins;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StreetCoinTransactionResponse {

    private UUID id;
    private Integer amount;
    private String currency;
    private String status;
    private String description;
    private String externalPaymentId;
    private String idempotencyKey;
    private LocalDateTime createdAt;
}
