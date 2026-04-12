package com.streetask.app.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StripeCheckoutSessionResponse {

    private String sessionId;
    private String checkoutUrl;
    private String publishableKey;
}
