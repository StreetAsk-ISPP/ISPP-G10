package com.streetask.app.report.payload.request;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.streetask.app.model.enums.QuestionReportReason;

class CreateQuestionReportRequestTest {

    @Test
    void shouldSetAndGetProperties() {
        CreateQuestionReportRequest request = new CreateQuestionReportRequest();

        UUID testId = UUID.randomUUID();
        QuestionReportReason testReason = QuestionReportReason.values()[0];
        String testDescription = "Test Description";

        request.setQuestionId(testId);
        request.setReason(testReason);
        request.setDescription(testDescription);

        assertThat(request.getQuestionId()).isEqualTo(testId);
        assertThat(request.getReason()).isEqualTo(testReason);
        assertThat(request.getDescription()).isEqualTo(testDescription);
    }
}