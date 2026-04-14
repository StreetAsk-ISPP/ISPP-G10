package com.streetask.app.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.streetask.app.business.BusinessAccount;
import com.streetask.app.answer.AnswerRepository;
import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.model.Question;
import com.streetask.app.question.QuestionRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;

@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    private UserRepository userRepository;
    private AnswerRepository answerRepository;
    private QuestionRepository questionRepository;
    private PasswordEncoder passwordEncoder;

    private static final int LIKE_WEIGHT = 2;
    private static final int DISLIKE_WEIGHT = 1;
    private static final int DEFAULT_REGULAR_PREMIUM_AMOUNT_CENTS = 999;

    @Value("${streetask.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${streetask.stripe.publishable-key:}")
    private String stripePublishableKey;

    @Value("${streetask.stripe.currency:eur}")
    private String stripeCurrency;

    @Value("${streetask.stripe.regular-premium-amount-cents:${STRIPE_REGULAR_PREMIUM_AMOUNT_CENTS:999}}")
    private Integer stripeRegularPremiumAmountCents;

    @Value("${FRONTEND_URL:http://localhost:8081}")
    private String frontendUrl;

    @Value("${streetask.stripe.success-url:${FRONTEND_URL:http://localhost:8081}}")
    private String stripeSuccessUrl;

    @Value("${streetask.stripe.cancel-url:${FRONTEND_URL:http://localhost:8081}}")
    private String stripeCancelUrl;

    @Autowired
    public UserService(UserRepository userRepository, AnswerRepository answerRepository,
            QuestionRepository questionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private PasswordEncoder getPasswordEncoder() {
        return passwordEncoder != null ? passwordEncoder : new BCryptPasswordEncoder();
    }

    @Transactional
    public User saveUser(User user) throws DataAccessException {
        userRepository.save(user);
        return enrichReputation(user);
    }

    @Transactional(readOnly = true)
    public User findUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return enrichReputation(user);
    }

    @Transactional(readOnly = true)
    public User findUser(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return enrichReputation(user);
    }

    @Transactional(readOnly = true)
    public User findCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            throw new ResourceNotFoundException("Nobody authenticated!");
        else {
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "Email", auth.getName()));
            return enrichReputation(user);
        }
    }

    public Boolean existsUser(String email) {
        return userRepository.existsByEmail(email);
    }

    public Boolean existsByUserName(String userName) {
        return userRepository.existsByUserName(userName);
    }

    @Transactional(readOnly = true)
    public Iterable<User> findAll() {
        return enrichReputation(userRepository.findAll());
    }

    public Iterable<User> findAllByAuthority(String auth) {
        return enrichReputation(userRepository.findAllByAuthority(auth));
    }

    @Transactional
    public User updateUser(@Valid User user, UUID idToUpdate) {
        User toUpdate = findUser(idToUpdate);

        String previousPassword = toUpdate.getPassword();

        BeanUtils.copyProperties(user, toUpdate, "id", "authority", "accountType", "createdAt", "lastLogin", "active");

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            toUpdate.setPassword(previousPassword);
        } else {
            toUpdate.setPassword(getPasswordEncoder().encode(user.getPassword()));
        }

        userRepository.save(toUpdate);
        return enrichReputation(toUpdate);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User toDelete = findUser(id);
        this.userRepository.delete(toDelete);
    }

    @Transactional
    public void deleteCurrentUserAccount() {
        User currentUser = findCurrentUser();
        deleteUserAccount(currentUser);
    }

    private void deleteUserAccount(User user) {
        UUID userId = user.getId();

        deleteByUserId("DELETE FROM password_reset_tokens WHERE user_id = :userId", userId);
        deleteByUserId("DELETE FROM user_role_change_logs WHERE user_id = :userId", userId);
        deleteByUserId("DELETE FROM feedback_messages WHERE user_id = :userId", userId);
        deleteByUserId("DELETE FROM user_locations WHERE user_id = :userId", userId);

        if (user instanceof RegularUser) {
            cleanupRegularUserData(userId);
        }

        if (user instanceof BusinessAccount) {
            cleanupBusinessUserData(userId);
        }

        if (user instanceof Admin) {
            cleanupAdminUserData(userId);
        }

        this.userRepository.delete(user);
    }

    private void cleanupRegularUserData(UUID userId) {
        // First remove references where this user is the actor.
        deleteByUserId("DELETE FROM answer_votes WHERE user_id = :userId", userId);
        deleteByUserId("DELETE FROM answer_reports WHERE reporter_id = :userId", userId);
        deleteByUserId("DELETE FROM question_reports WHERE reporter_id = :userId", userId);
        deleteByUserId("DELETE FROM reports WHERE reporter_id = :userId", userId);
        deleteByUserId("DELETE FROM coin_transactions WHERE user_id = :userId", userId);
        deleteByUserId("DELETE FROM notifications WHERE user_id = :userId", userId);
        deleteByUserId("DELETE FROM push_devices WHERE user_id = :userId", userId);
        deleteByUserId("DELETE FROM event_attendances WHERE regular_user_id = :userId", userId);
        deleteByUserId("DELETE FROM strikes WHERE user_id = :userId", userId);

        // Then remove references for this user's answers.
        deleteByUserId("DELETE FROM answer_reports WHERE answer_id IN (SELECT id FROM answers WHERE user_id = :userId)",
            userId);
        deleteByUserId("DELETE FROM answer_votes WHERE answer_id IN (SELECT id FROM answers WHERE user_id = :userId)",
            userId);
        deleteByUserId("DELETE FROM answers WHERE user_id = :userId", userId);

        // Finally remove references for this user's questions.
        deleteByUserId(
                "DELETE FROM answer_reports WHERE answer_id IN (SELECT a.id FROM answers a WHERE a.question_id IN (SELECT q.id FROM questions q WHERE q.creator_id = :userId))",
                userId);
        deleteByUserId(
                "DELETE FROM answer_votes WHERE answer_id IN (SELECT a.id FROM answers a WHERE a.question_id IN (SELECT q.id FROM questions q WHERE q.creator_id = :userId))",
                userId);
        deleteByUserId(
                "DELETE FROM answers WHERE question_id IN (SELECT q.id FROM questions q WHERE q.creator_id = :userId)",
                userId);
        deleteByUserId(
                "DELETE FROM question_reports WHERE question_id IN (SELECT id FROM questions WHERE creator_id = :userId)",
                userId);
        deleteByUserId("DELETE FROM questions WHERE creator_id = :userId", userId);
    }

    private void cleanupBusinessUserData(UUID userId) {
        deleteByUserId(
                "DELETE FROM answer_reports WHERE answer_id IN (SELECT a.id FROM answers a WHERE a.question_id IN (SELECT q.id FROM questions q WHERE q.event_id IN (SELECT e.id FROM events e WHERE e.creator_id = :userId)))",
                userId);
        deleteByUserId(
                "DELETE FROM answer_votes WHERE answer_id IN (SELECT a.id FROM answers a WHERE a.question_id IN (SELECT q.id FROM questions q WHERE q.event_id IN (SELECT e.id FROM events e WHERE e.creator_id = :userId)))",
                userId);
        deleteByUserId(
                "DELETE FROM answers WHERE question_id IN (SELECT q.id FROM questions q WHERE q.event_id IN (SELECT e.id FROM events e WHERE e.creator_id = :userId))",
                userId);
        deleteByUserId(
                "DELETE FROM question_reports WHERE question_id IN (SELECT q.id FROM questions q WHERE q.event_id IN (SELECT e.id FROM events e WHERE e.creator_id = :userId))",
                userId);
        deleteByUserId("DELETE FROM questions WHERE event_id IN (SELECT id FROM events WHERE creator_id = :userId)",
                userId);
        deleteByUserId(
                "DELETE FROM event_attendances WHERE event_id IN (SELECT id FROM events WHERE creator_id = :userId)",
                userId);
        deleteByUserId("DELETE FROM events WHERE creator_id = :userId", userId);
    }

    private void cleanupAdminUserData(UUID userId) {
        deleteByUserId("DELETE FROM report_admin_reviews WHERE admin_id = :userId", userId);
        deleteByUserId("UPDATE reports SET resolved_by_admin_id = NULL WHERE resolved_by_admin_id = :userId", userId);
        deleteByUserId("UPDATE business_accounts SET verified_by_id = NULL WHERE verified_by_id = :userId", userId);
        deleteByUserId("DELETE FROM strikes WHERE issued_by_id = :userId", userId);
    }

    private void deleteByUserId(String sql, UUID userId) {
        entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    private User enrichReputation(User user) {
        Map<UUID, Integer> reputationByUserId = calculateReputationByUserIds(List.of(user.getId()));
        user.setReputation(reputationByUserId.getOrDefault(user.getId(), 0));
        return user;
    }

    private Iterable<User> enrichReputation(Iterable<User> users) {
        List<User> userList = StreamSupport.stream(users.spliterator(), false).toList();
        if (userList.isEmpty()) {
            return userList;
        }

        List<UUID> userIds = new ArrayList<>(userList.size());
        for (User user : userList) {
            userIds.add(user.getId());
        }

        Map<UUID, Integer> reputationByUserId = calculateReputationByUserIds(userIds);

        for (User user : userList) {
            user.setReputation(reputationByUserId.getOrDefault(user.getId(), 0));
        }

        return userList;
    }

    private Map<UUID, Integer> calculateReputationByUserIds(List<UUID> userIds) {
        Map<UUID, Integer> reputationByUserId = new HashMap<>();
        List<Object[]> aggregates = answerRepository.aggregateVotesByUserIds(userIds);
        for (Object[] row : aggregates) {
            UUID userId = (UUID) row[0];
            int likes = ((Number) row[1]).intValue();
            int dislikes = ((Number) row[2]).intValue();
            int reputation = (likes * LIKE_WEIGHT) - (dislikes * DISLIKE_WEIGHT);
            reputationByUserId.put(userId, reputation);
        }
        return reputationByUserId;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats(UUID userId) {
        User user = findUser(userId);

        long questionsCount = questionRepository.countByCreatorId(userId);
        long answersCount = answerRepository.countByUserId(userId);

        int likesCount = 0;
        int dislikesCount = 0;
        int coinBalance = 0;
        if (user instanceof RegularUser regularUser) {
            likesCount = regularUser.getTotalLikesReceived() == null ? 0 : regularUser.getTotalLikesReceived();
            dislikesCount = regularUser.getTotalDislikesReceived() == null ? 0 : regularUser.getTotalDislikesReceived();
            coinBalance = regularUser.getCoinBalance() == null ? 0 : regularUser.getCoinBalance();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("questionsCount", questionsCount);
        stats.put("answersCount", answersCount);
        stats.put("username", user.getUserName());

        stats.put("bio", user.getBio());
        stats.put("profilePictureUrl", user.getProfilePictureUrl());

        stats.put("role", user.getAuthority().getAuthority());
        stats.put("likesCount", likesCount);
        stats.put("dislikesCount", dislikesCount);
        stats.put("coinBalance", coinBalance);
        int reputation = (likesCount * LIKE_WEIGHT) - (dislikesCount * DISLIKE_WEIGHT);
        stats.put("reputation", reputation);

        // Calculate rating on a 0-5 scale from vote ratio.
        // Formula: likes / (likes + dislikes) * 5
        int totalInteractions = likesCount + dislikesCount;
        double rating = 0.0;
        if (totalInteractions > 0) {
            rating = ((double) likesCount / (double) totalInteractions) * 5.0;
            if (rating > 5.0) {
                rating = 5.0;
            }
            rating = Math.round(rating * 10.0) / 10.0;
        }

        stats.put("rating", rating);

        return stats;
    }

    @Transactional(readOnly = true)
    public Iterable<Question> findQuestionsByUserId(UUID userId) {
        return questionRepository.findByCreatorId(userId);
    }

    @Transactional(readOnly = true)
    public Iterable<com.streetask.app.model.Answer> findAnswersByUserId(UUID userId) {
        return answerRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public StripeCheckoutSessionResponse createCurrentRegularPremiumStripeCheckoutSession() {
        RegularUser regularUser = getCurrentRegularUser();
        if (Boolean.TRUE.equals(regularUser.getPremiumActive())) {
            throw new AccessDeniedException("Regular premium access is already active.");
        }

        ensureStripeConfigured();
        Stripe.apiKey = stripeSecretKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(appendQuery(stripeSuccessUrl, "payment=success&session_id={CHECKOUT_SESSION_ID}"))
                .setCancelUrl(appendQuery(stripeCancelUrl, "payment=cancel"))
                .putMetadata("regularUserId", regularUser.getId().toString())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(normalizeCurrency())
                                                .setUnitAmount(resolveRegularPremiumAmount())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("StreetAsk Premium Plan")
                                                                .setDescription("Regular user premium activation")
                                                                .build())
                                                .build())
                                .build())
                .build();

        try {
            Session session = Session.create(params);
            return new StripeCheckoutSessionResponse(session.getId(), session.getUrl(), stripePublishableKey);
        } catch (StripeException ex) {
            throw new IllegalStateException("Unable to create Stripe checkout session.", ex);
        }
    }

    @Transactional
    public RegularUser confirmCurrentRegularPremiumStripeCheckoutSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("Stripe sessionId is required.");
        }

        RegularUser regularUser = getCurrentRegularUser();
        ensureStripeConfigured();
        Stripe.apiKey = stripeSecretKey;

        try {
            Session session = Session.retrieve(sessionId.trim());
            if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
                throw new AccessDeniedException("Payment has not been completed yet.");
            }

            String metadataRegularUserId = session.getMetadata() == null ? null
                    : session.getMetadata().get("regularUserId");
            if (!regularUser.getId().toString().equals(metadataRegularUserId)) {
                throw new AccessDeniedException("Stripe session does not belong to this regular account.");
            }

            regularUser.setPremiumActive(true);
            userRepository.save(regularUser);
            return regularUser;
        } catch (StripeException ex) {
            throw new IllegalStateException("Unable to confirm Stripe checkout session.", ex);
        }
    }

    @Transactional
    public RegularUser updateCurrentRegularPremiumAccess(boolean premiumActive) {
        RegularUser regularUser = getCurrentRegularUser();
        regularUser.setPremiumActive(premiumActive);
        userRepository.save(regularUser);
        return regularUser;
    }

    private RegularUser getCurrentRegularUser() {
        User currentUser = findCurrentUser();
        if (!(currentUser instanceof RegularUser regularUser)
                || currentUser.getAccountType() != AccountType.REGULAR_USER) {
            throw new AccessDeniedException("Only regular users can access this endpoint.");
        }
        return regularUser;
    }

    private void ensureStripeConfigured() {
        if (!StringUtils.hasText(stripeSecretKey)) {
            throw new IllegalStateException("Stripe secret key is not configured.");
        }
    }

    private String normalizeCurrency() {
        return StringUtils.hasText(stripeCurrency) ? stripeCurrency.trim().toLowerCase(Locale.ROOT) : "eur";
    }

    private Long resolveRegularPremiumAmount() {
        int amount = stripeRegularPremiumAmountCents == null
                ? DEFAULT_REGULAR_PREMIUM_AMOUNT_CENTS
                : stripeRegularPremiumAmountCents;
        return (long) Math.max(amount, 1);
    }

    private String appendQuery(String baseUrl, String query) {
        String safeBaseUrl = StringUtils.hasText(baseUrl)
                ? baseUrl.trim()
                : (StringUtils.hasText(frontendUrl) ? frontendUrl.trim() : "http://localhost:8081");
        String separator = safeBaseUrl.contains("?") ? "&" : "?";
        return safeBaseUrl + separator + query;
    }
}
