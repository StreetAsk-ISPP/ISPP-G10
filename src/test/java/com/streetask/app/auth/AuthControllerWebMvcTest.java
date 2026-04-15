package com.streetask.app.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetask.app.configuration.jwt.JwtUtils;
import com.streetask.app.user.UserService;

import org.springframework.security.authentication.AuthenticationManager;

@ExtendWith(MockitoExtension.class)
class AuthControllerWebMvcTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuthService authService;

    private Object businessAccountRepositoryMock;
    private final AtomicBoolean taxIdExists = new AtomicBoolean(false);

    @BeforeEach
    void setUp() throws Exception {
        Constructor<?> constructor = AuthController.class.getDeclaredConstructors()[0];
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType.equals(AuthenticationManager.class)) {
                args[i] = authenticationManager;
            } else if (parameterType.equals(UserService.class)) {
                args[i] = userService;
            } else if (parameterType.equals(JwtUtils.class)) {
                args[i] = jwtUtils;
            } else if (parameterType.equals(AuthService.class)) {
                args[i] = authService;
            } else if (parameterType.getSimpleName().equals("BusinessAccountRepository")) {
                businessAccountRepositoryMock = Mockito.mock(parameterType, invocation -> {
                    if ("existsByTaxId".equals(invocation.getMethod().getName())) {
                        return taxIdExists.get();
                    }
                    return Mockito.RETURNS_DEFAULTS.answer(invocation);
                });
                args[i] = businessAccountRepositoryMock;
            } else {
                args[i] = Mockito.mock(parameterType);
            }
        }

        AuthController authController = (AuthController) constructor.newInstance(args);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void signupBusinessShouldReturnBadRequestWhenBasicUserDoesNotExist() throws Exception {
        Map<String, Object> payload = validBusinessPayload("missing.user@streetask.com", "B12345678", "Address 1");

        when(authService.isPendingBasicSignup("missing.user@streetask.com")).thenReturn(false);

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

        when(authService.isPendingBasicSignup("business@streetask.com")).thenReturn(true);
        taxIdExists.set(true);

        mockMvc.perform(post("/api/v1/auth/signup/business")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Tax ID is already registered!"));
    }

    @Test
    void signupBusinessShouldReturnBadRequestWhenUserIsAlreadyBusinessAccount() throws Exception {
        Map<String, Object> payload = validBusinessPayload("business@streetask.com", "B12345678", "Address 1");

        when(authService.isPendingBasicSignup("business@streetask.com")).thenReturn(true);
        taxIdExists.set(false);
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

        when(authService.isPendingBasicSignup("business@streetask.com")).thenReturn(true);
        taxIdExists.set(false);
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

        when(authService.isPendingBasicSignup("business@streetask.com")).thenReturn(true);
        taxIdExists.set(false);

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
