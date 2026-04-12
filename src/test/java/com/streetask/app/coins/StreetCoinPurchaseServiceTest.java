package com.streetask.app.coins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.streetask.app.model.CoinTransaction;
import com.streetask.app.model.CoinTransactionRepository;
import com.streetask.app.model.enums.CoinTransactionStatus;
import com.streetask.app.model.enums.CoinTransactionType;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;
import com.streetask.app.user.UserService;

@ExtendWith(MockitoExtension.class)
class StreetCoinPurchaseServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private RegularUserRepository regularUserRepository;

    @Mock
    private CoinTransactionRepository coinTransactionRepository;

    @InjectMocks
    private StreetCoinPurchaseService streetCoinPurchaseService;

    private RegularUser currentUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        currentUser = new RegularUser();
        currentUser.setId(userId);
        currentUser.setEmail("user1@streetask.com");
        currentUser.setCoinBalance(5);

        ReflectionTestUtils.setField(streetCoinPurchaseService, "purchaseMode", "mock");
        ReflectionTestUtils.setField(streetCoinPurchaseService, "stripePublishableKey", "pk_test_123");

        when(userService.findCurrentUser()).thenReturn(currentUser);
        when(regularUserRepository.findById(userId)).thenReturn(Optional.of(currentUser));
    }

    @Test
    void createPurchase_shouldCreatePendingMockTransaction() {
        UUID transactionId = UUID.randomUUID();
        StreetCoinPurchaseRequest request = new StreetCoinPurchaseRequest();
        request.setPackId("PACK_2");

        when(coinTransactionRepository.findByUserIdAndIdempotencyKey(userId, "idem-1"))
                .thenReturn(Optional.empty());
        when(coinTransactionRepository.save(any(CoinTransaction.class))).thenAnswer(invocation -> {
            CoinTransaction tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(transactionId);
            }
            return tx;
        });

        StreetCoinPurchaseResponse response = streetCoinPurchaseService.createPurchase(request, "idem-1");

        assertNotNull(response);
        assertEquals(transactionId, response.getTransactionId());
        assertEquals("pending", response.getStatus());
        assertEquals(10, response.getStreetCoins());
        assertEquals("mock-" + transactionId, response.getSessionId());
        assertEquals("idem-1", response.getIdempotencyKey());
    }

    @Test
    void createPurchase_shouldReturnExistingTransactionForSameIdempotencyKey() {
        StreetCoinPurchaseRequest request = new StreetCoinPurchaseRequest();
        request.setPackId("PACK_1");

        UUID transactionId = UUID.randomUUID();
        CoinTransaction existing = new CoinTransaction();
        existing.setId(transactionId);
        existing.setUser(currentUser);
        existing.setType(CoinTransactionType.PURCHASE);
        existing.setStatus(CoinTransactionStatus.PENDING);
        existing.setAmount(4);
        existing.setCurrency("StreetCoins");
        existing.setIdempotencyKey("idem-existing");
        existing.setExternalPaymentId("mock-" + transactionId);

        when(coinTransactionRepository.findByUserIdAndIdempotencyKey(userId, "idem-existing"))
                .thenReturn(Optional.of(existing));

        StreetCoinPurchaseResponse response = streetCoinPurchaseService.createPurchase(request, "idem-existing");

        assertEquals(transactionId, response.getTransactionId());
        assertEquals("idem-existing", response.getIdempotencyKey());
        verify(coinTransactionRepository, never()).save(any(CoinTransaction.class));
    }

    @Test
    void confirmPurchase_shouldIncreaseBalanceWhenPendingMockTransaction() {
        UUID transactionId = UUID.randomUUID();
        String sessionId = "mock-" + transactionId;

        CoinTransaction pendingTx = new CoinTransaction();
        pendingTx.setId(transactionId);
        pendingTx.setUser(currentUser);
        pendingTx.setType(CoinTransactionType.PURCHASE);
        pendingTx.setStatus(CoinTransactionStatus.PENDING);
        pendingTx.setAmount(20);
        pendingTx.setExternalPaymentId(sessionId);

        StreetCoinPurchaseConfirmRequest request = new StreetCoinPurchaseConfirmRequest();
        request.setSessionId(sessionId);

        when(coinTransactionRepository.findByExternalPaymentId(sessionId)).thenReturn(Optional.of(pendingTx));
        when(coinTransactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(pendingTx));
        when(regularUserRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(currentUser));
        when(regularUserRepository.save(any(RegularUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(coinTransactionRepository.save(any(CoinTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StreetCoinPurchaseConfirmResponse response = streetCoinPurchaseService.confirmPurchase(request);

        assertEquals("success", response.getStatus());
        assertEquals(20, response.getAddedStreetCoins());
        assertEquals(25, response.getNewBalance());
        assertEquals(25, currentUser.getCoinBalance());
    }

    @Test
    void createPurchase_shouldRejectUnknownPack() {
        StreetCoinPurchaseRequest request = new StreetCoinPurchaseRequest();
        request.setPackId("PACK_UNKNOWN");

        when(coinTransactionRepository.findByUserIdAndIdempotencyKey(userId, "idem-invalid"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> streetCoinPurchaseService.createPurchase(request, "idem-invalid"));
    }
}
