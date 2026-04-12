package com.streetask.app.coins;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StreetCoinPackResponse {

    private String packId;
    private String label;
    private Integer amountCents;
    private Double amountEur;
    private Integer streetCoins;
}
