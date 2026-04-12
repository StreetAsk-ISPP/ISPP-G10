package com.streetask.app.coins;

import java.util.Arrays;

public enum StreetCoinPack {
    PACK_1("PACK_1", "Pack 1", 199, 4),
    PACK_2("PACK_2", "Pack 2", 399, 10),
    PACK_3("PACK_3", "Pack 3", 599, 20),
    PACK_4("PACK_4", "Pack 4", 999, 50);

    private final String id;
    private final String label;
    private final int amountCents;
    private final int streetCoins;

    StreetCoinPack(String id, String label, int amountCents, int streetCoins) {
        this.id = id;
        this.label = label;
        this.amountCents = amountCents;
        this.streetCoins = streetCoins;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getAmountCents() {
        return amountCents;
    }

    public int getStreetCoins() {
        return streetCoins;
    }

    public static StreetCoinPack fromId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Pack id is required.");
        }

        return Arrays.stream(values())
                .filter(pack -> pack.id.equalsIgnoreCase(id.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown StreetCoins pack: " + id));
    }
}
