package com.streetask.app.business;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class BusinessAccountPremiumAccessTest {

    @Test
    void premiumActiveShouldBeFalseWhenBusinessIsNotActivated() {
        BusinessAccount account = new BusinessAccount();
        account.setVerified(true);
        account.setSubscriptionActive(false);
        account.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(15));

        assertFalse(account.getPremiumActive());
    }

    @Test
    void premiumActiveShouldBeTrueWhenBusinessIsVerifiedAndSubscriptionIsActive() {
        BusinessAccount account = new BusinessAccount();
        account.setVerified(true);
        account.setSubscriptionActive(true);
        account.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(15));

        assertTrue(account.getPremiumActive());
    }
}
