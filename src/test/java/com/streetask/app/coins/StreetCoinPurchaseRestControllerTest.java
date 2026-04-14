package com.streetask.app.coins;

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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(StreetCoinPurchaseRestController.class)
@WithMockUser
class StreetCoinPurchaseRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StreetCoinPurchaseService streetCoinPurchaseService;

    private StreetCoinPurchaseResponse purchaseResponse;
    private StreetCoinPurchaseConfirmResponse confirmResponse;
    private StreetCoinBalanceResponse balanceResponse;

    @BeforeEach
    void setUp() {
        purchaseResponse = new StreetCoinPurchaseResponse(null, "pending", 10, "StreetCoins", "idem", "session", null,
                "pk");
        confirmResponse = new StreetCoinPurchaseConfirmResponse(null, "success", 10, 15, "session");
        balanceResponse = new StreetCoinBalanceResponse(null, 15, "StreetCoins");
    }

    @Test
    void getAvailablePacksShouldReturnOk() throws Exception {
        when(streetCoinPurchaseService.getAvailablePacks()).thenReturn(List.of(
                new StreetCoinPackResponse("PACK_1", "Pack 1", 399, 3.99, 4)));

        mockMvc.perform(get("/api/v1/streetcoins/packs")
                .contentType(APPLICATION_JSON)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].packId").value("PACK_1"));
    }

    @Test
    void getCurrentBalanceShouldReturnOk() throws Exception {
        when(streetCoinPurchaseService.getCurrentBalance()).thenReturn(balanceResponse);

        mockMvc.perform(get("/api/v1/streetcoins/balance")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(15));
    }

    @Test
    void getCurrentTransactionsShouldReturnOk() throws Exception {
        when(streetCoinPurchaseService.getCurrentTransactions()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/streetcoins/transactions")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getCurrentPurchasesShouldReturnOk() throws Exception {
        when(streetCoinPurchaseService.getCurrentPurchases()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/streetcoins/purchases")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createPurchaseShouldReturnCreatedAndDelegateHeader() throws Exception {
        when(streetCoinPurchaseService.createPurchase(any(StreetCoinPurchaseRequest.class), any()))
                .thenReturn(purchaseResponse);

        mockMvc.perform(post("/api/v1/streetcoins/purchase")
                .with(csrf())
                .header("Idempotency-Key", "idem-1")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createPurchaseRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.streetCoins").value(10));

        verify(streetCoinPurchaseService).createPurchase(any(StreetCoinPurchaseRequest.class), any());
    }

    @Test
    void confirmPurchaseShouldReturnOkAndDelegate() throws Exception {
        when(streetCoinPurchaseService.confirmPurchase(any(StreetCoinPurchaseConfirmRequest.class)))
                .thenReturn(confirmResponse);

        StreetCoinPurchaseConfirmRequest request = new StreetCoinPurchaseConfirmRequest();
        request.setSessionId("session");

        mockMvc.perform(post("/api/v1/streetcoins/purchase/confirm")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.newBalance").value(15));
    }

    private StreetCoinPurchaseRequest createPurchaseRequest() {
        StreetCoinPurchaseRequest request = new StreetCoinPurchaseRequest();
        request.setPackId("PACK_1");
        request.setIdempotencyKey("idem-1");
        return request;
    }
}