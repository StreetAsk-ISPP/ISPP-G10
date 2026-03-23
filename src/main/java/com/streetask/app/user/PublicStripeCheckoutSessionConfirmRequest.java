package com.streetask.app.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicStripeCheckoutSessionConfirmRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String taxId;

    @NotBlank
    private String sessionId;
}
