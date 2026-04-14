package com.streetask.app.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.streetask.app.model.Question;

@ExtendWith(MockitoExtension.class)
class QuestionRestControllerBranchTest {

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private QuestionRestController controller;

    @Test
    void findAll_shouldRouteToEveryFilterBranch() {
        UUID creatorId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Question question = new Question();

        when(questionService.findByCreatorAndEventAndActive(creatorId, eventId, true)).thenReturn(List.of(question));
        when(questionService.findByCreatorAndEvent(creatorId, eventId)).thenReturn(List.of(question));
        when(questionService.findByCreatorAndActive(creatorId, true)).thenReturn(List.of(question));
        when(questionService.findByEventAndActive(eventId, true)).thenReturn(List.of(question));
        when(questionService.findByCreator(creatorId)).thenReturn(List.of(question));
        when(questionService.findByEvent(eventId)).thenReturn(List.of(question));
        when(questionService.findByActive(true)).thenReturn(List.of(question));
        when(questionService.findAll()).thenReturn(List.of(question));

        assertThat(controller.findAll(creatorId, eventId, true).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.findAll(creatorId, eventId, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.findAll(creatorId, null, true).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.findAll(null, eventId, true).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.findAll(creatorId, null, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.findAll(null, eventId, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.findAll(null, null, true).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.findAll(null, null, null).getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(questionService).findByCreatorAndEventAndActive(creatorId, eventId, true);
        verify(questionService).findByCreatorAndEvent(creatorId, eventId);
        verify(questionService).findByCreatorAndActive(creatorId, true);
        verify(questionService).findByEventAndActive(eventId, true);
        verify(questionService).findByCreator(creatorId);
        verify(questionService).findByEvent(eventId);
        verify(questionService).findByActive(true);
        verify(questionService).findAll();
    }

    @Test
    void create_shouldMapRequestWithAndWithoutEvent() {
        UUID eventId = UUID.randomUUID();
        Question saved = new Question();
        saved.setId(UUID.randomUUID());
        saved.setTitle("Mapped");
        saved.setContent("Mapped content");

        CreateQuestionRequest withEvent = new CreateQuestionRequest();
        withEvent.setTitle("Mapped");
        withEvent.setContent("Mapped content");
        withEvent.setConfirmStreetCoinSpend(Boolean.TRUE);
        CreateQuestionRequest.EventReference eventRef = new CreateQuestionRequest.EventReference();
        eventRef.setId(eventId);
        withEvent.setEvent(eventRef);

        when(questionService.saveQuestion(any(Question.class), eq(Boolean.TRUE))).thenReturn(saved);
        ResponseEntity<Question> createdWithEvent = controller.create(withEvent);
        assertThat(createdWithEvent.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        CreateQuestionRequest noEvent = new CreateQuestionRequest();
        noEvent.setTitle("Mapped");
        noEvent.setContent("Mapped content");
        when(questionService.saveQuestion(any(Question.class), isNull())).thenReturn(saved);
        ResponseEntity<Question> createdWithoutEvent = controller.create(noEvent);
        assertThat(createdWithoutEvent.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void byIdTodayCountUpdateDelete_shouldDelegateToService() {
        UUID id = UUID.randomUUID();
        Question question = new Question();
        question.setId(id);
        question.setTitle("Q");

        when(questionService.findQuestion(id)).thenReturn(question);
        when(questionService.getTodayQuestionCountForAuthenticatedUser(id)).thenReturn(3L);
        when(questionService.updateQuestion(any(Question.class), eq(id))).thenReturn(question);

        assertThat(controller.findById(id).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getTodayQuestionCount(id).getBody()).isEqualTo(3L);
        assertThat(controller.update(id, question).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.delete(id).getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(questionService, times(3)).findQuestion(id);
        verify(questionService).getTodayQuestionCountForAuthenticatedUser(id);
        verify(questionService).updateQuestion(any(Question.class), eq(id));
        verify(questionService).deleteQuestion(id);
    }
}
