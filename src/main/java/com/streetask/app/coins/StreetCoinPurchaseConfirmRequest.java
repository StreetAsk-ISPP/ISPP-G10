package com.streetask.app.coins;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StreetCoinPurchaseConfirmRequest {

    @NotBlank
    private String sessionId;
}
