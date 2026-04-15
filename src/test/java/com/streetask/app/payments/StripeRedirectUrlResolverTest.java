package com.streetask.app.payments;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StripeRedirectUrlResolverTest {

    @Test
    void shouldUseRequestedReturnUrlWhenItMatchesAllowedPattern() {
        StripeRedirectUrlResolver resolver = new StripeRedirectUrlResolver(
                new String[] { "http://localhost:*", "https://streetask.expo.app" });

        String result = resolver.resolveCheckoutBaseUrl(
                "http://localhost:19006/app?payment=success#fragment",
                "http://localhost:8081");

        assertEquals("http://localhost:19006/app", result);
    }

    @Test
    void shouldFallbackWhenRequestedReturnUrlIsInvalid() {
        StripeRedirectUrlResolver resolver = new StripeRedirectUrlResolver(
                new String[] { "http://localhost:*" });

        String result = resolver.resolveCheckoutBaseUrl(
                "javascript:alert(1)",
                "https://streetask.expo.app");

        assertEquals("https://streetask.expo.app", result);
    }

    @Test
    void shouldAcceptRequestedReturnUrlWhenItSharesOriginWithConfiguredFallback() {
        StripeRedirectUrlResolver resolver = new StripeRedirectUrlResolver(
                new String[] { "http://localhost:*" });

        String result = resolver.resolveCheckoutBaseUrl(
                "https://myapp.example/checkout",
                "https://myapp.example/app");

        assertEquals("https://myapp.example/checkout", result);
    }

    @Test
    void shouldRejectRequestedReturnUrlWhenNotAllowed() {
        StripeRedirectUrlResolver resolver = new StripeRedirectUrlResolver(
                new String[] { "http://localhost:*" });

        String result = resolver.resolveCheckoutBaseUrl(
                "https://malicious.example/phishing",
                "https://streetask.expo.app");

        assertEquals("https://streetask.expo.app", result);
    }
}
