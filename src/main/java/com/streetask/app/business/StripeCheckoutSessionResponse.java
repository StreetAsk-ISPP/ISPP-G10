package com.streetask.app.business;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StripeCheckoutSessionResponse {

    private String sessionId;
    private String checkoutUrl;
    private String publishableKey;
}
