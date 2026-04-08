package com.streetask.app.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetask.app.configuration.jwt.JwtUtils;
import com.streetask.app.user.BusinessAccountRepository;
import com.streetask.app.user.UserService;

import org.springframework.security.authentication.AuthenticationManager;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private AuthService authService;

    @MockBean
    private BusinessAccountRepository businessAccountRepository;

    @Test
    void signupBusinessShouldReturnBadRequestWhenBasicUserDoesNotExist() throws Exception {
        Map<String, Object> payload = validBusinessPayload("missing.user@streetask.com", "B12345678", "Address 1");

        when(userService.existsUser("missing.user@streetask.com")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/signup/business")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Error: Basic user registration not found. Please complete the basic signup first."));
    }

    @Test
    void signupBusinessShouldReturnBadRequestWhenTaxIdAlreadyExists() throws Exception {
        Map<String, Object> payload = validBusinessPayload("business@streetask.com", "B12345678", "Address 1");

        when(userService.existsUser("business@streetask.com")).thenReturn(true);
        when(businessAccountRepository.existsByTaxId("B12345678")).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/signup/business")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Tax ID is already registered!"));
    }

    @Test
    void signupBusinessShouldReturnBadRequestWhenUserIsAlreadyBusinessAccount() throws Exception {
        Map<String, Object> payload = validBusinessPayload("business@streetask.com", "B12345678", "Address 1");

        when(userService.existsUser("business@streetask.com")).thenReturn(true);
        when(businessAccountRepository.existsByTaxId("B12345678")).thenReturn(false);
        doThrow(new IllegalStateException("User is already a business account."))
                .when(authService).convertToBusinessUser(any());

        mockMvc.perform(post("/api/v1/auth/signup/business")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: User is already a business account."));
    }

    @Test
    void signupBusinessShouldReturnBadRequestWhenConversionFails() throws Exception {
        Map<String, Object> payload = validBusinessPayload("business@streetask.com", "B12345678", "Address 1");

        when(userService.existsUser("business@streetask.com")).thenReturn(true);
        when(businessAccountRepository.existsByTaxId("B12345678")).thenReturn(false);
        doThrow(new RuntimeException("Unexpected conversion error"))
                .when(authService).convertToBusinessUser(any());

        mockMvc.perform(post("/api/v1/auth/signup/business")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: User not found or already completed!"));
    }

    @Test
    void signupBusinessShouldReturnOkWhenPayloadIsValid() throws Exception {
        Map<String, Object> payload = validBusinessPayload("business@streetask.com", "B12345678", "Address 1");

        when(userService.existsUser("business@streetask.com")).thenReturn(true);
        when(businessAccountRepository.existsByTaxId("B12345678")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/signup/business")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Business account registered successfully! Your account is pending admin verification."));

        verify(authService).convertToBusinessUser(any());
    }

    private Map<String, Object> validBusinessPayload(String email, String taxId, String address) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("taxId", taxId);
        payload.put("companyName", "Test Company");
        payload.put("address", address);
        return payload;
    }
}
