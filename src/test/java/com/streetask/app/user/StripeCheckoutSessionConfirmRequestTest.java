package com.streetask.app.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StripeCheckoutSessionConfirmRequestTest {

    @Test
    @DisplayName("Should create request and set/get sessionId")
    void testGettersAndSetters() {
        StripeCheckoutSessionConfirmRequest request = new StripeCheckoutSessionConfirmRequest();

        request.setSessionId("sess_12345");

        assertNotNull(request);
        assertEquals("sess_12345", request.getSessionId());
    }
}