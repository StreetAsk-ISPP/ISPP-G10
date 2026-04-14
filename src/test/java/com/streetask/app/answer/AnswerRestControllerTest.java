package com.streetask.app.answer;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetask.app.model.Answer;
import com.streetask.app.model.Question;
import com.streetask.app.model.enums.VoteType;
import com.streetask.app.question.QuestionService;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WebMvcTest(AnswerRestController.class)
class AnswerRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AnswerService answerService;

    @MockitoBean
    private QuestionService questionService;

    private UUID answerId;
    private UUID questionId;
    private UUID userId;
    private Answer answer;

    @BeforeEach
    void setUp() {
        answerId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        userId = UUID.randomUUID();

        answer = new Answer();
        answer.setId(answerId);
        answer.setContent("A complete and useful answer");
        answer.setUpvotes(0);
        answer.setDownvotes(0);

        Question question = new Question();
        question.setId(questionId);
        answer.setQuestion(question);
    }

    @Test
    @WithMockUser
    void findAll_withQuestionUserAndVerifiedUsesCombinedFilter() throws Exception {
        when(answerService.findByQuestionAndUserAndIsVerified(questionId, userId, true)).thenReturn(List.of(answer));

        mockMvc.perform(get("/api/v1/answers")
                .param("questionId", questionId.toString())
                .param("userId", userId.toString())
                .param("isVerified", "true")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(answerId.toString()));

        verify(answerService, times(1)).findByQuestionAndUserAndIsVerified(questionId, userId, true);
    }

    @Test
    @WithMockUser
    void findAll_withQuestionAndUserUsesQuestionUserFilter() throws Exception {
        when(answerService.findByQuestionAndUser(questionId, userId)).thenReturn(List.of(answer));

        mockMvc.perform(get("/api/v1/answers")
                .param("questionId", questionId.toString())
                .param("userId", userId.toString())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(answerService, times(1)).findByQuestionAndUser(questionId, userId);
    }

    @Test
    @WithMockUser
    void findAll_withQuestionAndVerifiedUsesQuestionVerifiedFilter() throws Exception {
        when(answerService.findByQuestionAndIsVerified(questionId, true)).thenReturn(List.of(answer));

        mockMvc.perform(get("/api/v1/answers")
                .param("questionId", questionId.toString())
                .param("isVerified", "true")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(answerService, times(1)).findByQuestionAndIsVerified(questionId, true);
    }

    @Test
    @WithMockUser
    void findAll_withUserAndVerifiedUsesUserVerifiedFilter() throws Exception {
        when(answerService.findByUserAndIsVerified(userId, false)).thenReturn(List.of(answer));

        mockMvc.perform(get("/api/v1/answers")
                .param("userId", userId.toString())
                .param("isVerified", "false")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(answerService, times(1)).findByUserAndIsVerified(userId, false);
    }

    @Test
    @WithMockUser
    void findAll_withQuestionUsesSortedSearch() throws Exception {
        when(answerService.findByQuestionSorted(questionId, "date_desc", 0, 5)).thenReturn(List.of(answer));

        mockMvc.perform(get("/api/v1/answers")
                .param("questionId", questionId.toString())
                .param("sort", "date_desc")
                .param("page", "0")
                .param("size", "5")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(answerService, times(1)).findByQuestionSorted(questionId, "date_desc", 0, 5);
    }

    @Test
    @WithMockUser
    void findAll_withUserOnlyUsesUserFilter() throws Exception {
        when(answerService.findByUser(userId)).thenReturn(List.of(answer));

        mockMvc.perform(get("/api/v1/answers")
                .param("userId", userId.toString())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(answerService, times(1)).findByUser(userId);
    }

    @Test
    @WithMockUser
    void findAll_withVerifiedOnlyUsesVerifiedFilter() throws Exception {
        when(answerService.findByIsVerified(true)).thenReturn(List.of(answer));

        mockMvc.perform(get("/api/v1/answers")
                .param("isVerified", "true")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(answerService, times(1)).findByIsVerified(true);
    }

    @Test
    @WithMockUser
    void findAll_withoutFiltersUsesFindAll() throws Exception {
        when(answerService.findAll()).thenReturn(List.of(answer));

        mockMvc.perform(get("/api/v1/answers")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(answerId.toString()));

        verify(answerService, times(1)).findAll();
    }

    @Test
    @WithMockUser
    void findById_returnsAnswer() throws Exception {
        when(answerService.findAnswer(answerId)).thenReturn(answer);

        mockMvc.perform(get("/api/v1/answers/{id}", answerId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(answerId.toString()))
                .andExpect(jsonPath("$.content").value("A complete and useful answer"));
    }

    @Test
    @WithMockUser
    void create_returnsNotFoundWhenQuestionIsMissingInPayload() throws Exception {
        Answer payload = new Answer();
        payload.setContent("A valid answer body");
        Question payloadQuestion = new Question();
        payloadQuestion.setId(questionId);
        payload.setQuestion(payloadQuestion);

        mockMvc.perform(post("/api/v1/answers")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());

        verify(questionService, never()).findQuestion(questionId);
        verify(answerService, never()).saveAnswer(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser
    void create_returnsNotFoundWhenQuestionCannotBeResolvedFromPayload() throws Exception {
        Answer payload = new Answer();
        payload.setContent("A valid answer body");
        Question payloadQuestion = new Question();
        payloadQuestion.setId(questionId);
        payload.setQuestion(payloadQuestion);

        mockMvc.perform(post("/api/v1/answers")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());

        verify(questionService, never()).findQuestion(questionId);
        verify(answerService, never()).saveAnswer(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser
    void update_returnsOkWhenValid() throws Exception {
        Answer payload = new Answer();
        payload.setContent("Updated content");

        when(answerService.findAnswer(answerId)).thenReturn(answer);
        when(answerService.updateAnswer(payload, answerId, answer.getQuestion())).thenReturn(answer);

        mockMvc.perform(put("/api/v1/answers/{answerId}", answerId)
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(answerId.toString()));
    }

    @Test
    @WithMockUser
    void update_returnsBadRequestWhenServiceRejects() throws Exception {
        Answer payload = new Answer();
        payload.setContent("Updated content");

        when(answerService.findAnswer(answerId)).thenReturn(answer);
        when(answerService.updateAnswer(payload, answerId, answer.getQuestion()))
                .thenThrow(new IllegalArgumentException("invalid location"));

        mockMvc.perform(put("/api/v1/answers/{answerId}", answerId)
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid location"));
    }

    @Test
    @WithMockUser
    void delete_returnsSuccessMessage() throws Exception {
        when(answerService.findAnswer(answerId)).thenReturn(answer);

        mockMvc.perform(delete("/api/v1/answers/{answerId}", answerId)
                .with(csrf())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Answer deleted!"));

        verify(answerService, times(1)).deleteAnswer(answerId);
    }

    @Test
    @WithMockUser
    void getUserVotes_returnsMap() throws Exception {
        when(answerService.getUserVotesForQuestion(userId, questionId))
                .thenReturn(Map.of(answerId, VoteType.LIKE.name()));

        mockMvc.perform(get("/api/v1/answers/votes")
                .param("userId", userId.toString())
                .param("questionId", questionId.toString())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['" + answerId.toString() + "']").value("LIKE"));
    }

    @Test
    @WithMockUser
    void updateVotes_returnsOkWhenVoteApplied() throws Exception {
        when(answerService.findAnswer(answerId)).thenReturn(answer);
        when(answerService.updateVotes(answerId, userId, VoteType.LIKE)).thenReturn(answer);

        mockMvc.perform(put("/api/v1/answers/{answerId}/votes", answerId)
                .with(csrf())
                .param("userId", userId.toString())
                .param("voteType", VoteType.LIKE.name())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(answerId.toString()));
    }

    @Test
    @WithMockUser
    void updateVotes_returnsBadRequestWhenServiceThrows() throws Exception {
        when(answerService.findAnswer(answerId)).thenReturn(answer);
        when(answerService.updateVotes(answerId, userId, VoteType.DISLIKE))
                .thenThrow(new IllegalArgumentException("self vote not allowed"));

        mockMvc.perform(put("/api/v1/answers/{answerId}/votes", answerId)
                .with(csrf())
                .param("userId", userId.toString())
                .param("voteType", VoteType.DISLIKE.name())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("self vote not allowed"));
    }

    @Test
    @WithMockUser
    void removeVote_returnsOkWhenVoteRemoved() throws Exception {
        when(answerService.findAnswer(answerId)).thenReturn(answer);
        when(answerService.removeVote(answerId, userId)).thenReturn(answer);

        mockMvc.perform(delete("/api/v1/answers/{answerId}/votes", answerId)
                .with(csrf())
                .param("userId", userId.toString())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(answerId.toString()));
    }

    @Test
    @WithMockUser
    void removeVote_returnsBadRequestWhenServiceThrows() throws Exception {
        when(answerService.findAnswer(answerId)).thenReturn(answer);
        when(answerService.removeVote(answerId, userId))
                .thenThrow(new IllegalArgumentException("no vote found"));

        mockMvc.perform(delete("/api/v1/answers/{answerId}/votes", answerId)
                .with(csrf())
                .param("userId", userId.toString())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("no vote found"));
    }

    @Test
    @WithMockUser
    void updateVotes_withMissingAnswerReturnsNotFoundAndSkipsVoteUpdate() throws Exception {
        when(answerService.findAnswer(answerId)).thenReturn(null);

        mockMvc.perform(put("/api/v1/answers/{answerId}/votes", answerId)
                .with(csrf())
                .param("userId", userId.toString())
                .param("voteType", VoteType.LIKE.name())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(answerService, never()).updateVotes(answerId, userId, VoteType.LIKE);
    }

    @Test
    void create_directCallReturnsCreatedWhenServiceSucceeds() {
        Answer payload = new Answer();
        payload.setContent("A valid answer body");
        Question payloadQuestion = new Question();
        payloadQuestion.setId(questionId);
        payload.setQuestion(payloadQuestion);

        Question existingQuestion = new Question();
        existingQuestion.setId(questionId);

        when(questionService.findQuestion(questionId)).thenReturn(existingQuestion);
        when(answerService.saveAnswer(payload, existingQuestion)).thenReturn(answer);

        AnswerRestController controller = new AnswerRestController(answerService, questionService);
        ResponseEntity<?> response = controller.create(payload);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(answer, response.getBody());
    }

    @Test
    void create_directCallReturnsBadRequestWhenServiceThrowsIllegalArgument() {
        Answer payload = new Answer();
        payload.setContent("A valid answer body");
        Question payloadQuestion = new Question();
        payloadQuestion.setId(questionId);
        payload.setQuestion(payloadQuestion);

        Question existingQuestion = new Question();
        existingQuestion.setId(questionId);

        when(questionService.findQuestion(questionId)).thenReturn(existingQuestion);
        when(answerService.saveAnswer(payload, existingQuestion))
                .thenThrow(new IllegalArgumentException("outside allowed radius"));

        AnswerRestController controller = new AnswerRestController(answerService, questionService);
        ResponseEntity<?> response = controller.create(payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
