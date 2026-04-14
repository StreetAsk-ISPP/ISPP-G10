package com.streetask.app.functionalities.feedback;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(FeedbackMessageController.class)
class FeedbackMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedbackMessageService feedbackMessageService;

    @Test
    @WithMockUser
    void createFeedback_shouldReturnCreated() throws Exception {
        FeedbackMessage created = new FeedbackMessage();
        created.setMessage("hello");
        created.setType(FeedbackType.BUG);
        created.setUserName("tester");

        when(feedbackMessageService.createFeedback(any(FeedbackMessageRequest.class))).thenReturn(created);

        Map<String, Object> body = Map.of("message", "hello", "type", "BUG");

        mockMvc.perform(post("/api/v1/feedback")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("hello")))
                .andExpect(jsonPath("$.type", is("BUG")));

        verify(feedbackMessageService).createFeedback(any(FeedbackMessageRequest.class));
    }

    @Test
    @WithMockUser
    void getFeedbackMessages_shouldReturnOk() throws Exception {
        FeedbackMessage message = new FeedbackMessage();
        message.setMessage("hello");
        message.setType(FeedbackType.SUGGESTION);

        when(feedbackMessageService.findAllOrderedByCreatedAtDesc()).thenReturn(List.of(message));

        mockMvc.perform(get("/api/v1/feedback").contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message", is("hello")));
    }

    @Test
    @WithMockUser
    void getFeedbackMessageById_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        FeedbackMessage message = new FeedbackMessage();
        message.setMessage("one");
        message.setType(FeedbackType.OTHER);

        when(feedbackMessageService.findById(id)).thenReturn(message);

        mockMvc.perform(get("/api/v1/feedback/{id}", id).contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("one")));

        verify(feedbackMessageService).findById(id);
    }

    @Test
    @WithMockUser
    void deleteFeedbackMessage_shouldReturnNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(feedbackMessageService).deleteById(eq(id));

        mockMvc.perform(delete("/api/v1/feedback/{id}", id)
                .with(csrf())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(feedbackMessageService).deleteById(id);
    }
}
