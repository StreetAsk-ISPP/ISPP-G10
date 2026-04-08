package com.streetask.app.business;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MockBusinessSubscriptionActivationRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String taxId;

    @Positive
    private Integer durationDays;
}
