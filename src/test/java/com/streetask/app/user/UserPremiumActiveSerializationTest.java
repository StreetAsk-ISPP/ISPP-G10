package com.streetask.app.user;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.streetask.app.business.BusinessAccount;

class UserPremiumActiveSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void regularUserShouldExposePremiumActiveInJson() throws Exception {
        RegularUser regularUser = new RegularUser();
        regularUser.setPremiumActive(false);

        String json = objectMapper.writeValueAsString(regularUser);

        assertTrue(json.contains("\"premiumActive\":false"));
    }

    @Test
    void regularPremiumUserShouldExposePremiumActiveInJson() throws Exception {
        RegularUser regularUser = new RegularUser();
        regularUser.setPremiumActive(true);

        String json = objectMapper.writeValueAsString(regularUser);

        assertTrue(json.contains("\"premiumActive\":true"));
    }

    @Test
    void businessAccountShouldExposeDerivedPremiumActiveInJson() throws Exception {
        BusinessAccount businessAccount = new BusinessAccount();
        businessAccount.setVerified(true);
        businessAccount.setSubscriptionActive(true);
        businessAccount.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(7));

        String json = objectMapper.writeValueAsString(businessAccount);

        assertTrue(json.contains("\"premiumActive\":true"));
    }
}
