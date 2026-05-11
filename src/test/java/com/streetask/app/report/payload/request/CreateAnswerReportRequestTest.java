package com.streetask.app.report.payload.request;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.streetask.app.model.enums.AnswerReportReason;

class CreateAnswerReportRequestTest {

    @Test
    void shouldSetAndGetProperties() {
        CreateAnswerReportRequest request = new CreateAnswerReportRequest();

        UUID testId = UUID.randomUUID();
        AnswerReportReason testReason = AnswerReportReason.values()[0];
        String testDescription = "Test Description";

        request.setAnswerId(testId);
        request.setReason(testReason);
        request.setDescription(testDescription);

        assertThat(request.getAnswerId()).isEqualTo(testId);
        assertThat(request.getReason()).isEqualTo(testReason);
        assertThat(request.getDescription()).isEqualTo(testDescription);
    }
}