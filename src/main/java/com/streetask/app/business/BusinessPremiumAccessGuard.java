package com.streetask.app.business;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.streetask.app.exceptions.AccessDeniedException;

@Component
public class BusinessPremiumAccessGuard {

    public void requireVerified(BusinessAccount account) {
        if (!Boolean.TRUE.equals(account.getVerified())) {
            throw new AccessDeniedException("Business account must be verified.");
        }
    }

    public void requirePremiumAccess(BusinessAccount account) {
        requireVerified(account);

        if (!Boolean.TRUE.equals(account.getSubscriptionActive())) {
            throw new AccessDeniedException("Business subscription is not active.");
        }

        LocalDateTime expiresAt = account.getSubscriptionExpiresAt();
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            throw new AccessDeniedException("Business subscription is expired.");
        }
    }

    public boolean hasPremiumAccess(BusinessAccount account) {
        return Boolean.TRUE.equals(account.getVerified())
                && Boolean.TRUE.equals(account.getSubscriptionActive())
                && account.getSubscriptionExpiresAt() != null
                && account.getSubscriptionExpiresAt().isAfter(LocalDateTime.now());
    }
}
