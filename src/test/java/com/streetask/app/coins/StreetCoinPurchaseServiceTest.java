package com.streetask.app.coins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.streetask.app.model.CoinTransaction;
import com.streetask.app.model.CoinTransactionRepository;
import com.streetask.app.model.enums.CoinTransactionStatus;
import com.streetask.app.model.enums.CoinTransactionType;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;
import com.streetask.app.user.UserService;
import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.exceptions.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
        ReflectionTestUtils.setField(streetCoinPurchaseService, "stripeSecretKey", "sk_test_123");

        when(userService.findCurrentUser()).thenReturn(currentUser);
        when(regularUserRepository.findById(userId)).thenReturn(Optional.of(currentUser));
    }

    @Test
    void getAvailablePacksShouldExposeAllPacks() {
        List<StreetCoinPackResponse> packs = streetCoinPurchaseService.getAvailablePacks();

        assertEquals(true, packs.size() >= 1);
        assertEquals("PACK_1", packs.get(0).getPackId());
    }

    @Test
    void getCurrentBalanceShouldReturnSafeBalanceForCurrentUser() {
        StreetCoinBalanceResponse response = streetCoinPurchaseService.getCurrentBalance();

        assertEquals(userId, response.getUserId());
        assertEquals(5, response.getBalance());
    }

    @Test
    void getCurrentTransactionsShouldReturnMappedTransactions() {
        CoinTransaction tx = new CoinTransaction();
        tx.setId(UUID.randomUUID());
        tx.setUser(currentUser);
        tx.setType(CoinTransactionType.EARN);
        tx.setStatus(CoinTransactionStatus.SUCCESS);
        tx.setAmount(3);
        tx.setCurrency("StreetCoins");
        tx.setCreatedAt(LocalDateTime.now());

        when(coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(tx));

        List<StreetCoinTransactionResponse> response = streetCoinPurchaseService.getCurrentTransactions();

        assertEquals(1, response.size());
        assertEquals("earn", response.get(0).getType());
        assertEquals("Coins earned", response.get(0).getDescription());
    }

    @Test
    void getCurrentPurchasesShouldReturnMappedPurchaseTransactions() {
        CoinTransaction tx = new CoinTransaction();
        tx.setId(UUID.randomUUID());
        tx.setUser(currentUser);
        tx.setType(CoinTransactionType.PURCHASE);
        tx.setStatus(CoinTransactionStatus.SUCCESS);
        tx.setAmount(10);
        tx.setCurrency("StreetCoins");
        tx.setCreatedAt(LocalDateTime.now());

        when(coinTransactionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, CoinTransactionType.PURCHASE))
                .thenReturn(List.of(tx));

        List<StreetCoinTransactionResponse> response = streetCoinPurchaseService.getCurrentPurchases();

        assertEquals(1, response.size());
        assertEquals("purchase", response.get(0).getType());
        assertEquals("StreetCoins purchase", response.get(0).getDescription());
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

    @Test
    void createPurchaseShouldRejectMissingIdempotencyKeyWhenNoBodyKeyProvided() {
        StreetCoinPurchaseRequest request = new StreetCoinPurchaseRequest();
        request.setPackId("PACK_1");

        assertThrows(IllegalArgumentException.class,
                () -> streetCoinPurchaseService.createPurchase(request, " "));
    }

    @Test
    void createPurchaseShouldReturnExistingPurchaseByBodyIdempotencyKey() {
        StreetCoinPurchaseRequest request = new StreetCoinPurchaseRequest();
        request.setPackId("PACK_1");
        request.setIdempotencyKey("idem-body");

        CoinTransaction existing = new CoinTransaction();
        existing.setId(UUID.randomUUID());
        existing.setUser(currentUser);
        existing.setType(CoinTransactionType.PURCHASE);
        existing.setStatus(CoinTransactionStatus.PENDING);
        existing.setAmount(4);
        existing.setCurrency("StreetCoins");
        existing.setExternalPaymentId("mock-1");
        existing.setIdempotencyKey("idem-body");

        when(coinTransactionRepository.findByUserIdAndIdempotencyKey(userId, "idem-body"))
                .thenReturn(Optional.of(existing));

        StreetCoinPurchaseResponse response = streetCoinPurchaseService.createPurchase(request, null);

        assertEquals(existing.getId(), response.getTransactionId());
        verify(coinTransactionRepository, never()).save(any(CoinTransaction.class));
    }

    @Test
    void createPurchaseShouldUseMockModeWhenConfigured() {
        ReflectionTestUtils.setField(streetCoinPurchaseService, "purchaseMode", "mock");

        StreetCoinPurchaseRequest request = new StreetCoinPurchaseRequest();
        request.setPackId("PACK_1");

        when(coinTransactionRepository.findByUserIdAndIdempotencyKey(userId, "idem-2"))
                .thenReturn(Optional.empty());
        when(coinTransactionRepository.save(any(CoinTransaction.class))).thenAnswer(invocation -> {
            CoinTransaction tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });

        StreetCoinPurchaseResponse response = streetCoinPurchaseService.createPurchase(request, "idem-2");

        assertEquals("pending", response.getStatus());
        assertEquals(null, response.getCheckoutUrl());
    }

    @Test
    void confirmPurchaseShouldRejectMissingSessionId() {
        StreetCoinPurchaseConfirmRequest request = new StreetCoinPurchaseConfirmRequest();
        request.setSessionId(" ");

        assertThrows(IllegalArgumentException.class, () -> streetCoinPurchaseService.confirmPurchase(request));
    }

    @Test
    void requireCurrentRegularUserShouldRejectNonRegularCurrentUser() {
        when(userService.findCurrentUser()).thenReturn(new com.streetask.app.business.BusinessAccount());

        assertThrows(AccessDeniedException.class, () -> streetCoinPurchaseService.getCurrentBalance());
    }

    @Test
    void confirmPurchaseShouldRejectMissingMockTransaction() {
        ReflectionTestUtils.setField(streetCoinPurchaseService, "purchaseMode", "mock");
        StreetCoinPurchaseConfirmRequest request = new StreetCoinPurchaseConfirmRequest();
        request.setSessionId("mock-missing");

        when(coinTransactionRepository.findByExternalPaymentId("mock-missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> streetCoinPurchaseService.confirmPurchase(request));
    }

    @Test
    void resolveDescriptionShouldCoverDefaultBranch() {
        CoinTransaction tx = new CoinTransaction();
        tx.setType(null);
        tx.setDescription(null);

        String description = ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "resolveDescription", tx);

        assertEquals("Transaction", description);
    }

    @Test
    void helperMethodsShouldCoverValidationAndNormalizationBranches() {
        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "requireSessionId", " "));
        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "resolveIdempotencyKey", " ", " "));

        assertEquals("idem-header", ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "resolveIdempotencyKey",
                "idem-header", null));
        assertEquals("idem-body", ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "resolveIdempotencyKey",
                null, "idem-body"));

        assertEquals(false, ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "isStripeMode"));
        ReflectionTestUtils.setField(streetCoinPurchaseService, "purchaseMode", "stripe");
        assertEquals(true, ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "isStripeMode"));

        ReflectionTestUtils.setField(streetCoinPurchaseService, "stripeCurrency", null);
        assertEquals("eur", ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "resolveStripeCurrency"));
        ReflectionTestUtils.setField(streetCoinPurchaseService, "stripeCurrency", "usd");
        assertEquals("usd", ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "resolveStripeCurrency"));
        ReflectionTestUtils.setField(streetCoinPurchaseService, "stripeCurrency", "eur");

        ReflectionTestUtils.setField(streetCoinPurchaseService, "stripeSecretKey", "");
        assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "ensureStripeConfigured"));
        ReflectionTestUtils.setField(streetCoinPurchaseService, "stripeSecretKey", "sk_test_123");

        assertEquals("http://localhost:8081?a=b",
                ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "appendQuery", "http://localhost:8081",
                        "a=b"));
        assertEquals("http://localhost:8081?x=1&a=b",
                ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "appendQuery", "http://localhost:8081?x=1",
                        "a=b"));

        UUID parsed = ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "parseUuid",
                userId.toString(), "bad uuid");
        assertEquals(userId, parsed);
        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "parseUuid", "bad", "bad uuid"));
    }

    @Test
    void completeTransactionShouldCoverSuccessAndErrorBranches() {
        UUID transactionId = UUID.randomUUID();
        CoinTransaction pendingTx = new CoinTransaction();
        pendingTx.setId(transactionId);
        pendingTx.setUser(currentUser);
        pendingTx.setType(CoinTransactionType.PURCHASE);
        pendingTx.setStatus(CoinTransactionStatus.PENDING);
        pendingTx.setAmount(10);
        pendingTx.setCurrency("StreetCoins");
        pendingTx.setCreatedAt(LocalDateTime.now());

        when(coinTransactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(pendingTx));
        when(regularUserRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(currentUser));
        when(regularUserRepository.save(any(RegularUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(coinTransactionRepository.save(any(CoinTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StreetCoinPurchaseConfirmResponse success = ReflectionTestUtils.invokeMethod(streetCoinPurchaseService,
                "completeTransaction", userId, transactionId, "session-1");

        assertEquals("success", success.getStatus());
        assertEquals(15, success.getNewBalance());

        CoinTransaction sameTx = new CoinTransaction();
        sameTx.setId(UUID.randomUUID());
        sameTx.setUser(currentUser);
        sameTx.setType(CoinTransactionType.PURCHASE);
        sameTx.setStatus(CoinTransactionStatus.SUCCESS);
        sameTx.setAmount(10);
        sameTx.setBalanceAfter(40);
        sameTx.setExternalPaymentId("done");
        when(coinTransactionRepository.findByIdForUpdate(sameTx.getId())).thenReturn(Optional.of(sameTx));

        StreetCoinPurchaseConfirmResponse alreadyDone = ReflectionTestUtils.invokeMethod(streetCoinPurchaseService,
                "completeTransaction", userId, sameTx.getId(), "session-2");
        assertEquals(40, alreadyDone.getNewBalance());

        CoinTransaction failedTx = new CoinTransaction();
        failedTx.setId(UUID.randomUUID());
        failedTx.setUser(currentUser);
        failedTx.setType(CoinTransactionType.PURCHASE);
        failedTx.setStatus(CoinTransactionStatus.FAILED);
        failedTx.setAmount(10);
        when(coinTransactionRepository.findByIdForUpdate(failedTx.getId())).thenReturn(Optional.of(failedTx));
        assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "completeTransaction", userId,
                        failedTx.getId(), "session-3"));

        CoinTransaction wrongType = new CoinTransaction();
        wrongType.setId(UUID.randomUUID());
        wrongType.setUser(currentUser);
        wrongType.setType(CoinTransactionType.EARN);
        wrongType.setStatus(CoinTransactionStatus.PENDING);
        wrongType.setAmount(10);
        when(coinTransactionRepository.findByIdForUpdate(wrongType.getId())).thenReturn(Optional.of(wrongType));
        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "completeTransaction", userId,
                        wrongType.getId(), "session-4"));

        CoinTransaction invalidAmount = new CoinTransaction();
        invalidAmount.setId(UUID.randomUUID());
        invalidAmount.setUser(currentUser);
        invalidAmount.setType(CoinTransactionType.PURCHASE);
        invalidAmount.setStatus(CoinTransactionStatus.PENDING);
        invalidAmount.setAmount(0);
        when(coinTransactionRepository.findByIdForUpdate(invalidAmount.getId())).thenReturn(Optional.of(invalidAmount));
        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "completeTransaction", userId,
                        invalidAmount.getId(), "session-5"));
    }

    @Test
    void findMockTransactionShouldResolveMockPrefixAndMissingEntries() {
        UUID transactionId = UUID.randomUUID();
        CoinTransaction tx = new CoinTransaction();
        tx.setId(transactionId);
        when(coinTransactionRepository.findByExternalPaymentId("mock-" + transactionId)).thenReturn(Optional.empty());
        when(coinTransactionRepository.findById(transactionId)).thenReturn(Optional.of(tx));

        CoinTransaction result = ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "findMockTransaction",
                "mock-" + transactionId);

        assertEquals(transactionId, result.getId());

        when(coinTransactionRepository.findByExternalPaymentId("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(streetCoinPurchaseService, "findMockTransaction", "missing"));
    }

    @Test
    void getCurrentPurchases_shouldReturnPurchaseTransactionsForAuthenticatedUser() {
        CoinTransaction latestPurchase = new CoinTransaction();
        latestPurchase.setId(UUID.randomUUID());
        latestPurchase.setUser(currentUser);
        latestPurchase.setType(CoinTransactionType.PURCHASE);
        latestPurchase.setStatus(CoinTransactionStatus.SUCCESS);
        latestPurchase.setAmount(20);
        latestPurchase.setCurrency("StreetCoins");
        latestPurchase.setCreatedAt(LocalDateTime.now());

        CoinTransaction olderPurchase = new CoinTransaction();
        olderPurchase.setId(UUID.randomUUID());
        olderPurchase.setUser(currentUser);
        olderPurchase.setType(CoinTransactionType.PURCHASE);
        olderPurchase.setStatus(CoinTransactionStatus.PENDING);
        olderPurchase.setAmount(10);
        olderPurchase.setCurrency("StreetCoins");
        olderPurchase.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        when(coinTransactionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, CoinTransactionType.PURCHASE))
                .thenReturn(List.of(latestPurchase, olderPurchase));

        List<StreetCoinTransactionResponse> result = streetCoinPurchaseService.getCurrentPurchases();

        assertEquals(2, result.size());
        assertEquals(latestPurchase.getId(), result.get(0).getId());
        assertEquals("success", result.get(0).getStatus());
        assertEquals("purchase", result.get(0).getType());
        assertEquals(20, result.get(0).getAmount());
    }

    @Test
    void getCurrentTransactions_shouldExposeTransactionType() {
        CoinTransaction spendTransaction = new CoinTransaction();
        spendTransaction.setId(UUID.randomUUID());
        spendTransaction.setUser(currentUser);
        spendTransaction.setType(CoinTransactionType.SPEND);
        spendTransaction.setStatus(CoinTransactionStatus.SUCCESS);
        spendTransaction.setAmount(-3);
        spendTransaction.setCurrency("StreetCoins");
        spendTransaction.setCreatedAt(LocalDateTime.now());

        when(coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(spendTransaction));

        List<StreetCoinTransactionResponse> result = streetCoinPurchaseService.getCurrentTransactions();

        assertEquals(1, result.size());
        assertEquals("spend", result.get(0).getType());
    }
}
