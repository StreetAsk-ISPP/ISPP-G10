package com.streetask.app.business;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StripeCheckoutSessionConfirmRequest {

    @NotBlank
    private String sessionId;
}
