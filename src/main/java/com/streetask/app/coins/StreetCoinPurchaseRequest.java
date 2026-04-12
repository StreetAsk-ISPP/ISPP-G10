package com.streetask.app.coins;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StreetCoinPurchaseRequest {

    @NotBlank
    private String packId;

    private String idempotencyKey;
}
