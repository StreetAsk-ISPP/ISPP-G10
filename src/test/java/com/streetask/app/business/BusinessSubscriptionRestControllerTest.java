package com.streetask.app.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetask.app.auth.payload.response.MessageResponse;

@WebMvcTest(BusinessSubscriptionRestController.class)
@WithMockUser
class BusinessSubscriptionRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BusinessSubscriptionService businessSubscriptionService;

    @MockitoBean
    private com.streetask.app.payments.CheckoutReturnUrlRequestResolver checkoutReturnUrlRequestResolver;

    private BusinessSubscriptionStatusResponse responseBody;

    @BeforeEach
    void setUp() {
        responseBody = new BusinessSubscriptionStatusResponse(
                UUID.randomUUID(), "biz@streetask.com", "Biz Co", true,
                RequestStatus.PENDING, null, true, null, true);
    }

    @Test
    void activateMockSubscriptionShouldReturnOkAndDelegate() throws Exception {
        when(businessSubscriptionService.activateMockSubscription("biz@streetask.com", "B12345678", 15))
                .thenReturn(responseBody);

        mockMvc.perform(post("/api/v1/business-subscriptions/mock/activate")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "biz@streetask.com",
                        "taxId", "B12345678",
                        "durationDays", 15))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("biz@streetask.com"))
                .andExpect(jsonPath("$.companyName").value("Biz Co"));

        verify(businessSubscriptionService).activateMockSubscription("biz@streetask.com", "B12345678", 15);
    }

    @Test
    void createStripeCheckoutSessionShouldReturnOkAndDelegate() throws Exception {
        StripeCheckoutSessionResponse checkoutResponse = new StripeCheckoutSessionResponse("session", "url", "pk");
        when(businessSubscriptionService.createPublicStripeCheckoutSession(any(), any(), any(), any(), any(), any(),
                any(), any()))
                .thenReturn(checkoutResponse);

        mockMvc.perform(post("/api/v1/business-subscriptions/stripe/checkout-session")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "biz@streetask.com",
                        "taxId", "B12345678",
                        "companyName", "Biz Co",
                        "address", "Address",
                        "website", "https://biz.co",
                        "description", "Desc",
                        "durationDays", 12))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session"))
                .andExpect(jsonPath("$.publishableKey").value("pk"));
    }

    @Test
    void confirmStripeCheckoutSessionShouldReturnOkAndDelegate() throws Exception {
        when(businessSubscriptionService.confirmPublicStripeCheckoutSession(any()))
                .thenReturn(responseBody);

        mockMvc.perform(post("/api/v1/business-subscriptions/stripe/confirm-session")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "biz@streetask.com",
                        "taxId", "B12345678",
                        "sessionId", "session"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessId").value(responseBody.getBusinessId().toString()));
    }

    @Test
    void getCurrentBusinessSubscriptionStatusShouldReturnOk() throws Exception {
        when(businessSubscriptionService.getCurrentBusinessStatus()).thenReturn(responseBody);

        mockMvc.perform(get("/api/v1/business-subscriptions/me")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("biz@streetask.com"));
    }

    @Test
    void activateCurrentBusinessMockSubscriptionShouldSupportNullRequest() throws Exception {
        when(businessSubscriptionService.activateCurrentBusinessMockSubscription(null)).thenReturn(responseBody);

        mockMvc.perform(post("/api/v1/business-subscriptions/me/mock/activate")
                .with(csrf())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Biz Co"));
    }

    @Test
    void createCurrentBusinessStripeCheckoutSessionShouldSupportNullRequest() throws Exception {
        StripeCheckoutSessionResponse checkoutResponse = new StripeCheckoutSessionResponse("session", "url", "pk");
        when(businessSubscriptionService.createCurrentBusinessStripeCheckoutSession(any(), any())).thenReturn(checkoutResponse);

        mockMvc.perform(post("/api/v1/business-subscriptions/me/stripe/checkout-session")
                .with(csrf())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session"));
    }

    @Test
    void confirmCurrentBusinessStripeCheckoutSessionShouldReturnOk() throws Exception {
        when(businessSubscriptionService.confirmCurrentBusinessStripeCheckoutSession("session"))
                .thenReturn(responseBody);

        mockMvc.perform(post("/api/v1/business-subscriptions/me/stripe/confirm-session")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sessionId", "session"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Biz Co"));
    }

    @Test
    void checkPremiumAccessShouldReturnGrantedMessage() throws Exception {
        mockMvc.perform(get("/api/v1/business-subscriptions/premium/access-check")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Premium access granted."));

        verify(businessSubscriptionService).requireCurrentBusinessPremiumAccess();
    }
}
