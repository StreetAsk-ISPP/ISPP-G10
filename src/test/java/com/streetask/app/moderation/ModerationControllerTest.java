package com.streetask.app.moderation;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.model.Strike;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.User;

@WebMvcTest(ModerationController.class)
@DisplayName("ModerationController Unit Tests")
class ModerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ModerationService moderationService;

    private UUID adminId;
    private UUID regularUserId;
    private User adminUser;
    private RegularUser regularUser;

    private static final String ADMIN_EMAIL = "admin@test.com";
    private static final String REGULAR_USER_EMAIL = "user@test.com";

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        regularUserId = UUID.randomUUID();

        adminUser = new User();
        adminUser.setId(adminId);
        adminUser.setEmail(ADMIN_EMAIL);
        Authorities adminAuthority = new Authorities();
        adminAuthority.setAuthority("ADMIN");
        adminUser.setAuthority(adminAuthority);

        regularUser = new RegularUser();
        regularUser.setId(regularUserId);
        regularUser.setEmail(REGULAR_USER_EMAIL);
        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");
        regularUser.setAuthority(userAuthority);
    }

    // ================= SEND STRIKE TESTS =================

    @Test
    @DisplayName("POST /api/v1/moderation/users/{userId}/strike should send strike successfully")
    @WithMockUser(username = "admin@test.com", authorities = "ADMIN")
    void sendStrike_shouldReturnCreatedStatus() throws Exception {
        // Arrange
        StrikeRequest request = new StrikeRequest();
        request.setReason("Inappropriate content");
        request.setDescription("Violated community guidelines");

        Strike createdStrike = new Strike();
        createdStrike.setId(UUID.randomUUID());
        createdStrike.setUser(regularUser);
        createdStrike.setIssuedBy(adminUser);
        createdStrike.setReason(request.getReason());
        createdStrike.setDescription(request.getDescription());
        createdStrike.setIssuedAt(LocalDateTime.now());

        when(moderationService.issueStrike(
            eq(regularUserId),
            eq(request.getReason()),
            eq(request.getDescription())
        )).thenReturn(createdStrike);

        // Act & Assert
        mockMvc.perform(post("/api/v1/moderation/users/{userId}/strike", regularUserId)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Strike sent successfully. Strike ID: " + createdStrike.getId()));

        verify(moderationService).issueStrike(
            eq(regularUserId),
            eq(request.getReason()),
            eq(request.getDescription())
        );
    }

    @Test
    @DisplayName("POST /api/v1/moderation/users/{userId}/strike should return bad request when reason is empty")
    @WithMockUser(username = "admin@test.com", authorities = "ADMIN")
    void sendStrike_shouldReturnBadRequestWhenReasonIsEmpty() throws Exception {
        // Arrange
        StrikeRequest request = new StrikeRequest();
        request.setReason(""); // Empty reason
        request.setDescription("Some description");

        // Act & Assert
        mockMvc.perform(post("/api/v1/moderation/users/{userId}/strike", regularUserId)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/moderation/users/{userId}/strike should return forbidden when user is not admin")
    @WithMockUser(username = "user@test.com", authorities = "USER")
    void sendStrike_shouldReturnForbiddenWhenNotAdmin() throws Exception {
        // Arrange
        StrikeRequest request = new StrikeRequest();
        request.setReason("Spam");
        request.setDescription("");

        when(moderationService.issueStrike(any(), any(), any()))
            .thenThrow(new AccessDeniedException("Only admins can send strikes"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/moderation/users/{userId}/strike", regularUserId)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/moderation/users/{userId}/strike should return unauthorized when not authenticated")
    void sendStrike_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        // Arrange
        StrikeRequest request = new StrikeRequest();
        request.setReason("Spam");

        // Act & Assert
        mockMvc.perform(post("/api/v1/moderation/users/{userId}/strike", regularUserId)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ================= DELETE USER TESTS =================

    @Test
    @DisplayName("DELETE /api/v1/moderation/users/{userId} should delete user successfully with confirmation")
    @WithMockUser(username = "admin@test.com", authorities = "ADMIN")
    void deleteUser_shouldDeleteSuccessfullyWithConfirmation() throws Exception {
        // Arrange
        doNothing().when(moderationService).deleteRegularUser(regularUserId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/moderation/users/{userId}", regularUserId)
                .param("confirm", "true")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User account deleted successfully."));

        verify(moderationService).deleteRegularUser(regularUserId);
    }

    @Test
    @DisplayName("DELETE /api/v1/moderation/users/{userId} should return bad request without confirmation")
    @WithMockUser(username = "admin@test.com", authorities = "ADMIN")
    void deleteUser_shouldReturnBadRequestWithoutConfirmation() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/moderation/users/{userId}", regularUserId)
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Deletion requires confirmation. Use confirm=true."));
    }

    @Test
    @DisplayName("DELETE /api/v1/moderation/users/{userId} should return bad request when confirm is false")
    @WithMockUser(username = "admin@test.com", authorities = "ADMIN")
    void deleteUser_shouldReturnBadRequestWhenConfirmIsFalse() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/moderation/users/{userId}", regularUserId)
                .param("confirm", "false")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Deletion requires confirmation. Use confirm=true."));
    }

    @Test
    @DisplayName("DELETE /api/v1/moderation/users/{userId} should return forbidden when user is not admin")
    @WithMockUser(username = "user@test.com", authorities = "USER")
    void deleteUser_shouldReturnForbiddenWhenNotAdmin() throws Exception {
        // Arrange
        doThrow(new AccessDeniedException("Only admins can delete users"))
            .when(moderationService).deleteRegularUser(regularUserId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/moderation/users/{userId}", regularUserId)
                .param("confirm", "true")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/moderation/users/{userId} should return unauthorized when not authenticated")
    void deleteUser_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/moderation/users/{userId}", regularUserId)
                .param("confirm", "true")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ================= GET STRIKE COUNT TESTS =================

    @Test
    @DisplayName("GET /api/v1/moderation/users/{userId}/strike-count should return strike count")
    @WithMockUser(username = "admin@test.com", authorities = "ADMIN")
    void getStrikeCount_shouldReturnStrikeCount() throws Exception {
        // Arrange
        when(moderationService.getStrikeCount(regularUserId)).thenReturn(3L);

        // Act & Assert
        mockMvc.perform(get("/api/v1/moderation/users/{userId}/strike-count", regularUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(3)));

        verify(moderationService).getStrikeCount(regularUserId);
    }

    @Test
    @DisplayName("GET /api/v1/moderation/users/{userId}/strike-count should return zero when user has no strikes")
    @WithMockUser(username = "admin@test.com", authorities = "ADMIN")
    void getStrikeCount_shouldReturnZeroWhenNoStrikes() throws Exception {
        // Arrange
        when(moderationService.getStrikeCount(regularUserId)).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/v1/moderation/users/{userId}/strike-count", regularUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/moderation/users/{userId}/strike-count should return forbidden when not admin")
    @WithMockUser(username = "user@test.com", authorities = "USER")
    void getStrikeCount_shouldReturnForbiddenWhenNotAdmin() throws Exception {
        // Arrange
        when(moderationService.getStrikeCount(regularUserId))
            .thenThrow(new AccessDeniedException("Only admins can view strike counts"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/moderation/users/{userId}/strike-count", regularUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/moderation/users/{userId}/strike-count should return unauthorized when not authenticated")
    void getStrikeCount_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/moderation/users/{userId}/strike-count", regularUserId))
                .andExpect(status().isUnauthorized());
    }

    // ================= GET USER STRIKES TESTS =================

    @Test
    @DisplayName("GET /api/v1/moderation/users/{userId}/strikes should return all strikes for user")
    @WithMockUser(username = "admin@test.com", authorities = "ADMIN")
    void getUserStrikes_shouldReturnAllStrikes() throws Exception {
        // Arrange
        List<Strike> strikes = Arrays.asList(
            createStrike(adminUser, regularUser, "Spam"),
            createStrike(adminUser, regularUser, "Abuse")
        );

        when(moderationService.getUserStrikes(regularUserId)).thenReturn(strikes);

        // Act & Assert
        mockMvc.perform(get("/api/v1/moderation/users/{userId}/strikes", regularUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].reason", is("Spam")))
                .andExpect(jsonPath("$[1].reason", is("Abuse")));

        verify(moderationService).getUserStrikes(regularUserId);
    }

    @Test
    @DisplayName("GET /api/v1/moderation/users/{userId}/strikes should return empty array when no strikes")
    @WithMockUser(username = "admin@test.com", authorities = "ADMIN")
    void getUserStrikes_shouldReturnEmptyArrayWhenNoStrikes() throws Exception {
        // Arrange
        when(moderationService.getUserStrikes(regularUserId)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/v1/moderation/users/{userId}/strikes", regularUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/moderation/users/{userId}/strikes should return forbidden when not admin")
    @WithMockUser(username = "user@test.com", authorities = "USER")
    void getUserStrikes_shouldReturnForbiddenWhenNotAdmin() throws Exception {
        // Arrange
        when(moderationService.getUserStrikes(regularUserId))
            .thenThrow(new AccessDeniedException("Only admins can view strikes"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/moderation/users/{userId}/strikes", regularUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/moderation/users/{userId}/strikes should return unauthorized when not authenticated")
    void getUserStrikes_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/moderation/users/{userId}/strikes", regularUserId))
                .andExpect(status().isUnauthorized());
    }

    // ================= HELPER METHODS =================

    private Strike createStrike(User issuedBy, RegularUser user, String reason) {
        Strike strike = new Strike();
        strike.setId(UUID.randomUUID());
        strike.setIssuedBy(issuedBy);
        strike.setUser(user);
        strike.setReason(reason);
        strike.setIssuedAt(LocalDateTime.now());
        return strike;
    }
}
