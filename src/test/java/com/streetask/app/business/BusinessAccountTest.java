package com.streetask.app.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class BusinessAccountTest {

    @Test
    void premiumActiveShouldBeFalseWhenAnyRequirementIsMissing() {
        BusinessAccount account = new BusinessAccount();
        account.setVerified(true);
        account.setSubscriptionActive(true);
        account.setSubscriptionExpiresAt(null);

        assertThat(account.getPremiumActive()).isFalse();
    }

    @Test
    void premiumActiveShouldBeTrueWhenAllRequirementsAreMet() {
        BusinessAccount account = new BusinessAccount();
        account.setVerified(true);
        account.setSubscriptionActive(true);
        account.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(10));

        assertThat(account.getPremiumActive()).isTrue();
    }
}