package com.streetask.app.functionalities.email;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.springframework.test.util.ReflectionTestUtils;

import com.sendgrid.Request;
import com.sendgrid.SendGrid;

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

    @Test
    void sendAccountDeletionEmail_shouldCatchAndLogExceptionWhenApiThrows() {
        EmailService service = new EmailService();
        ReflectionTestUtils.setField(service, "sendgridApiKey", "dummy-key");

        try (MockedConstruction<SendGrid> mockedSendGrid = Mockito.mockConstruction(SendGrid.class,
                (mock, context) -> {
                    when(mock.api(any(Request.class))).thenThrow(new IOException("Simulated network error"));
                })) {
            assertDoesNotThrow(() -> service.sendAccountDeletionEmail("user@test.com"));
            assertThat(mockedSendGrid.constructed()).hasSize(1);
        }
    }
}
