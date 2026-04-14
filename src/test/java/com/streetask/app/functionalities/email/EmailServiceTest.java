package com.streetask.app.functionalities.email;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EmailServiceTest {

    @Test
    void sendAccountDeletionEmail_shouldNotThrowWithRegularApiKey() {
        EmailService service = new EmailService();
        ReflectionTestUtils.setField(service, "sendgridApiKey", "test-key");

        assertDoesNotThrow(() -> service.sendAccountDeletionEmail("user@test.com"));
    }

    @Test
    void sendAccountDeletionEmail_shouldNotThrowWithBlankApiKey() {
        EmailService service = new EmailService();
        ReflectionTestUtils.setField(service, "sendgridApiKey", "");

        assertDoesNotThrow(() -> service.sendAccountDeletionEmail("user@test.com"));
    }
}
