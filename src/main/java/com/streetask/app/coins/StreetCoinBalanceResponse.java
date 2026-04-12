package com.streetask.app.coins;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StreetCoinBalanceResponse {

    private UUID userId;
    private Integer balance;
    private String currency;
}
