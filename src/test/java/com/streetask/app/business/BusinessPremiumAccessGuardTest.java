package com.streetask.app.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.streetask.app.exceptions.AccessDeniedException;

class BusinessPremiumAccessGuardTest {

    private final BusinessPremiumAccessGuard guard = new BusinessPremiumAccessGuard();

    @Test
    void requireVerifiedShouldAllowVerifiedAccount() {
        BusinessAccount account = new BusinessAccount();
        account.setVerified(true);

        guard.requireVerified(account);
        assertThat(account.getVerified()).isTrue();
    }

    @Test
    void requireVerifiedShouldRejectUnverifiedAccount() {
        BusinessAccount account = new BusinessAccount();
        account.setVerified(false);

        assertThatThrownBy(() -> guard.requireVerified(account))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Business account must be verified.");
    }

    @Test
    void requirePremiumAccessShouldRejectInactiveSubscription() {
        BusinessAccount account = new BusinessAccount();
        account.setVerified(true);
        account.setSubscriptionActive(false);
        account.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> guard.requirePremiumAccess(account))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Business subscription is not active.");
    }

    @Test
    void requirePremiumAccessShouldRejectExpiredSubscription() {
        BusinessAccount account = new BusinessAccount();
        account.setVerified(true);
        account.setSubscriptionActive(true);
        account.setSubscriptionExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> guard.requirePremiumAccess(account))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Business subscription is expired.");
    }

    @Test
    void hasPremiumAccessShouldReflectEffectiveAccess() {
        BusinessAccount account = new BusinessAccount();
        account.setVerified(true);
        account.setSubscriptionActive(true);
        account.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(1));

        assertThat(guard.hasPremiumAccess(account)).isTrue();
    }
}