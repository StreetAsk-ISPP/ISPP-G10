package com.streetask.app.user;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BusinessSubscriptionStatusResponse {

    private UUID businessId;
    private String email;
    private String companyName;
    private Boolean verified;
    private Boolean subscriptionActive;
    private LocalDateTime subscriptionExpiresAt;
    private Boolean premiumEligible;
}
