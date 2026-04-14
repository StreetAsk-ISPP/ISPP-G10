package com.streetask.app.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetask.app.auth.payload.response.MessageResponse;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.model.Answer;
import com.streetask.app.model.Question;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.User;
import com.streetask.app.user.UserService;
import com.streetask.app.user.AuthoritiesService;

@SpringBootTest
@AutoConfigureMockMvc
class UserRestControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private UserService userService;

        @MockBean
        private AuthoritiesService authService;

        @Test
        @WithMockUser(authorities = { "ADMIN" })
        void findAll_shouldReturnAllUsersForAdmin() throws Exception {
                User firstUser = createUser(UUID.randomUUID(), "first@example.com", "first", "USER");
                User secondUser = createUser(UUID.randomUUID(), "second@example.com", "second", "ADMIN");

                when(userService.findAll()).thenReturn(List.of(firstUser, secondUser));

                mockMvc.perform(get("/api/v1/users"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].email").value("first@example.com"))
                                .andExpect(jsonPath("$[1].userName").value("second"));
        }

        @Test
        @WithMockUser(authorities = { "ADMIN" })
        void findAll_withAuthParam_shouldReturnFilteredUsersForAdmin() throws Exception {
                User regularUser = createUser(UUID.randomUUID(), "user@example.com", "user", "USER");
                when(userService.findAllByAuthority("USER")).thenReturn(List.of(regularUser));

                mockMvc.perform(get("/api/v1/users").param("auth", "USER"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                                .andExpect(jsonPath("$[0].authority.authority").value("USER"));
        }

        @Test
        @WithMockUser(authorities = { "ADMIN" })
        void findAllAuthorities_shouldReturnAuthoritiesForAdmin() throws Exception {
                Authorities admin = new Authorities();
                admin.setId(UUID.randomUUID());
                admin.setAuthority("ADMIN");

                Authorities user = new Authorities();
                user.setId(UUID.randomUUID());
                user.setAuthority("USER");

                when(authService.findAll()).thenReturn(List.of(admin, user));

                mockMvc.perform(get("/api/v1/users/authorities"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].authority").value("ADMIN"))
                                .andExpect(jsonPath("$[1].authority").value("USER"));
        }

        @Test
        @WithMockUser(authorities = { "ADMIN" })
        void findById_shouldReturnUserForAdmin() throws Exception {
                UUID userId = UUID.randomUUID();
                User user = createUser(userId, "detail@example.com", "detail", "USER");
                when(userService.findUser(userId)).thenReturn(user);

                mockMvc.perform(get("/api/v1/users/{id}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(userId.toString()))
                                .andExpect(jsonPath("$.email").value("detail@example.com"));
        }

        @Test
        @WithMockUser(authorities = { "ADMIN" })
        void create_shouldPersistUserForAdmin() throws Exception {
                User payload = createUser(null, "created@example.com", "created", "USER");
                User savedUser = createUser(UUID.randomUUID(), "created@example.com", "created", "USER");
                when(userService.saveUser(any(User.class))).thenReturn(savedUser);

                mockMvc.perform(post("/api/v1/users")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(savedUser.getId().toString()))
                                .andExpect(jsonPath("$.userName").value("created"));
        }

        @Test
        @WithMockUser(authorities = { "ADMIN" })
        void update_shouldUpdateUserForAdmin() throws Exception {
                UUID userId = UUID.randomUUID();
                User existingUser = createUser(userId, "before@example.com", "before", "USER");
                User payload = createUser(null, "after@example.com", "after", "USER");
                User updatedUser = createUser(userId, "after@example.com", "after", "USER");

                User adminUser = createUser(UUID.randomUUID(), "admin@example.com", "admin", "ADMIN");
                when(userService.findCurrentUser()).thenReturn(adminUser);

                when(userService.findUser(userId)).thenReturn(existingUser);
                when(userService.updateUser(any(User.class), eq(userId))).thenReturn(updatedUser);

                mockMvc.perform(put("/api/v1/users/{userId}", userId)
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("after@example.com"))
                                .andExpect(jsonPath("$.userName").value("after"));
        }

        @Test
        @WithMockUser(username = "owner@example.com", authorities = { "USER" })
        void update_shouldUpdateOwnProfileSuccessfully() throws Exception {
                UUID userId = UUID.randomUUID();
                User currentUser = createUser(userId, "owner@example.com", "owner", "USER");
                User payload = createUser(null, "updated@example.com", "updated", "USER");
                User updatedUser = createUser(userId, "updated@example.com", "updated", "USER");

                when(userService.findCurrentUser()).thenReturn(currentUser);
                when(userService.findUser(userId)).thenReturn(currentUser);
                when(userService.updateUser(any(User.class), eq(userId))).thenReturn(updatedUser);

                mockMvc.perform(put("/api/v1/users/{userId}", userId)
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("updated@example.com"))
                                .andExpect(jsonPath("$.userName").value("updated"));
        }

        @Test
        @WithMockUser(username = "owner@example.com", authorities = { "USER" })
        void update_withInvalidPayload_shouldReturnBadRequest() throws Exception {
                UUID userId = UUID.randomUUID();

                User invalidPayload = new User();
                invalidPayload.setEmail("");
                invalidPayload.setUserName("");
                invalidPayload.setFirstName("");
                invalidPayload.setLastName("");

                mockMvc.perform(put("/api/v1/users/{userId}", userId)
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidPayload)))
                                .andExpect(status().isBadRequest());

                verify(userService, never()).updateUser(any(User.class), eq(userId));
        }

        @Test
        @WithMockUser(username = "hacker@example.com", authorities = { "USER" })
        void update_whenEditingOtherProfile_shouldReturnForbidden() throws Exception {
                UUID targetUserId = UUID.randomUUID();
                UUID hackerId = UUID.randomUUID();

                User hackerUser = createUser(hackerId, "hacker@example.com", "hacker", "USER");
                User targetUser = createUser(targetUserId, "target@example.com", "target", "USER");
                User payload = createUser(null, "hacked@example.com", "hacked", "USER");

                when(userService.findCurrentUser()).thenReturn(hackerUser);
                when(userService.findUser(targetUserId)).thenReturn(targetUser);

                mockMvc.perform(put("/api/v1/users/{userId}", targetUserId)
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isForbidden());

                verify(userService, never()).updateUser(any(User.class), eq(targetUserId));
        }

        @Test // No authentication provided
        void update_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
                UUID userId = UUID.randomUUID();
                User payload = createUser(null, "updated@example.com", "updated", "USER");

                mockMvc.perform(put("/api/v1/users/{userId}", userId)
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = { "ADMIN" })
        void delete_shouldDeleteOtherUserForAdmin() throws Exception {
                UUID targetUserId = UUID.randomUUID();
                UUID currentUserId = UUID.randomUUID();

                when(userService.findUser(targetUserId))
                                .thenReturn(createUser(targetUserId, "target@example.com", "target", "USER"));
                when(userService.findCurrentUser())
                                .thenReturn(createUser(currentUserId, "admin@example.com", "admin", "ADMIN"));

                mockMvc.perform(delete("/api/v1/users/{userId}", targetUserId))
                                .andExpect(status().isOk())
                                .andExpect(content().json(
                                                objectMapper.writeValueAsString(new MessageResponse("User deleted!"))));

                verify(userService).deleteUser(targetUserId);
        }

        @Test
        @WithMockUser(authorities = { "ADMIN" })
        void delete_whenDeletingSelf_shouldReturnForbidden() throws Exception {
                UUID currentUserId = UUID.randomUUID();
                User currentUser = createUser(currentUserId, "admin@example.com", "admin", "ADMIN");

                when(userService.findUser(currentUserId)).thenReturn(currentUser);
                when(userService.findCurrentUser()).thenReturn(currentUser);

                mockMvc.perform(delete("/api/v1/users/{userId}", currentUserId))
                                .andExpect(status().isForbidden());

                verify(userService, never()).deleteUser(currentUserId);
        }

        @Test
        @WithMockUser(username = "owner@example.com", authorities = { "USER" })
        void deleteCurrentUserAccount_shouldDeleteAuthenticatedAccount() throws Exception {
                UUID currentUserId = UUID.randomUUID();
                User currentUser = createUser(currentUserId, "owner@example.com", "owner", "USER");

                when(userService.findCurrentUser()).thenReturn(currentUser);

                mockMvc.perform(delete("/api/v1/users/me"))
                                .andExpect(status().isOk())
                                .andExpect(content().json(
                                                objectMapper.writeValueAsString(new MessageResponse(
                                                                "Your account has been deleted."))));

                verify(userService).deleteCurrentUserAccount();
        }

        @Test
        @WithMockUser(authorities = { "USER" })
        void statsEndpoint_shouldReturnStatsForAuthenticatedUser() throws Exception {
                UUID userId = UUID.randomUUID();
                when(userService.findUser(userId)).thenReturn(createUser(userId, "stats@example.com", "stats", "USER"));
                when(userService.getUserStats(userId)).thenReturn(Map.of(
                                "questionsCount", 2,
                                "answersCount", 5,
                                "username", "stats",
                                "role", "USER",
                                "likesCount", 7,
                                "dislikesCount", 1,
                                "reputation", 13));

                mockMvc.perform(get("/api/v1/users/{id}/stats", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("stats"))
                                .andExpect(jsonPath("$.reputation").value(13));
        }

        @Test
        @WithMockUser(authorities = { "USER" })
        void statsEndpoint_whenUserDoesNotExist_shouldReturnNotFound() throws Exception {
                UUID userId = UUID.randomUUID();
                when(userService.findUser(userId)).thenThrow(new ResourceNotFoundException("User", "ID", userId));

                mockMvc.perform(get("/api/v1/users/{id}/stats", userId))
                                .andExpect(status().isNotFound());

                verify(userService, never()).getUserStats(userId);
        }

        @Test
        @WithMockUser(authorities = { "USER" })
        void getUserQuestions_shouldReturnQuestionsForAuthenticatedUser() throws Exception {
                UUID userId = UUID.randomUUID();
                Question question = new Question();
                question.setId(UUID.randomUUID());
                question.setTitle("How does this work?");
                question.setContent("Question content");

                when(userService.findUser(userId)).thenReturn(createUser(userId, "owner@example.com", "owner", "USER"));
                when(userService.findQuestionsByUserId(userId)).thenReturn(List.of(question));

                mockMvc.perform(get("/api/v1/users/{id}/questions", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("How does this work?"));
        }

        @Test
        @WithMockUser(authorities = { "USER" })
        void getUserAnswers_shouldReturnAnswersForAuthenticatedUser() throws Exception {
                UUID userId = UUID.randomUUID();
                Answer answer = new Answer();
                answer.setId(UUID.randomUUID());
                answer.setContent("This is an answer");

                when(userService.findUser(userId)).thenReturn(createUser(userId, "owner@example.com", "owner", "USER"));
                when(userService.findAnswersByUserId(userId)).thenReturn(List.of(answer));

                mockMvc.perform(get("/api/v1/users/{id}/answers", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].content").value("This is an answer"));
        }

        @Test
        void statsEndpoint_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
                mockMvc.perform(get("/api/v1/users/{id}/stats", UUID.randomUUID()))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void deleteCurrentUserAccount_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
                mockMvc.perform(delete("/api/v1/users/me"))
                                .andExpect(status().isUnauthorized());

                verify(userService, never()).deleteCurrentUserAccount();
        }

        @Test
        @WithMockUser(authorities = { "USER" })
        void adminOnlyEndpoints_shouldRemainForbiddenForRegularUsers() throws Exception {
                mockMvc.perform(get("/api/v1/users"))
                                .andExpect(status().isForbidden());

                mockMvc.perform(post("/api/v1/users")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                createUser(null, "created@example.com", "created", "USER"))))
                                .andExpect(status().isForbidden());
        }

        private User createUser(UUID id, String email, String userName, String authorityName) {
                User user = new User();
                user.setId(id);
                user.setEmail(email);
                user.setUserName(userName);
                user.setFirstName("Test");
                user.setLastName("User");
                user.setPassword("password123");
                user.setActive(true);
                user.setCreatedAt(LocalDateTime.now());

                Authorities authority = new Authorities();
                authority.setId(UUID.randomUUID());
                authority.setAuthority(authorityName);
                user.setAuthority(authority);
                return user;
        }
}
