package com.streetask.app.answer;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.functionalities.notifications.events.AnswerCreatedEvent;
import com.streetask.app.business.BusinessAccount;
import com.streetask.app.model.Answer;
import com.streetask.app.model.AnswerVote;
import com.streetask.app.model.CoinTransaction;
import com.streetask.app.model.CoinTransactionRepository;
import com.streetask.app.model.GeoPoint;
import com.streetask.app.model.Question;
import com.streetask.app.model.enums.CoinTransactionStatus;
import com.streetask.app.model.enums.CoinTransactionType;
import com.streetask.app.model.enums.VoteType;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;
import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;

import jakarta.validation.Valid;

@Service
public class AnswerService {

    public static final String SORT_LIKES_DESC = "likes_desc";
    public static final String SORT_DATE_DESC = "date_desc";

    private final AnswerRepository answerRepository;
    private final AnswerVoteRepository answerVoteRepository;
    private final RegularUserRepository regularUserRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CoinTransactionRepository coinTransactionRepository;

    private static final int ANSWER_BASE_REWARD = 1;
    private static final int ANSWER_POSITIVE_VOTE_BONUS = 1;
    private static final int ANSWER_NEGATIVE_VOTE_PENALTY = -1;
    private static final int MIN_ANSWER_LENGTH = 10;
    private static final int MAX_ANSWERS_IN_WINDOW = 5;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 5;

    @Autowired
    public AnswerService(AnswerRepository answerRepository, AnswerVoteRepository answerVoteRepository,
            RegularUserRepository regularUserRepository, UserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            CoinTransactionRepository coinTransactionRepository) {
        this.answerRepository = answerRepository;
        this.answerVoteRepository = answerVoteRepository;
        this.regularUserRepository = regularUserRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.coinTransactionRepository = coinTransactionRepository;
    }

    @Transactional
    public Answer saveAnswer(@Valid Answer answer, Question question) throws DataAccessException {
        attachAuthenticatedUser(answer);
        validateAnswerLocation(answer, question);
        validateAnswerRateLimit(answer);
        applyDefaults(answer);
        applyVerificationStatus(answer);
        Answer savedAnswer = answerRepository.save(answer);
        savedAnswer = reconcileAnswerCoins(savedAnswer);
        eventPublisher.publishEvent(new AnswerCreatedEvent(answer.getId()));
        return savedAnswer;
    }

    @Transactional(readOnly = true)
    public Answer findAnswer(UUID id) {
        return answerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Answer", "id", id));
    }

    @Transactional(readOnly = true)
    public Iterable<Answer> findAll() {
        return answerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Iterable<Answer> findByQuestion(UUID questionId) {
        return answerRepository.findByQuestionId(questionId);
    }

    @Transactional(readOnly = true)
    public List<Answer> findByQuestionSorted(UUID questionId, String sort, Integer page, Integer size) {
        String normalizedSort = normalizeSort(sort);

        if (page == null || size == null) {
            if (SORT_DATE_DESC.equals(normalizedSort)) {
                return answerRepository.findByQuestionIdOrderByCreatedAtDesc(questionId);
            }
            return answerRepository.findByQuestionIdOrderByUpvotesDescCreatedAtDesc(questionId);
        }

        if (size <= 0) {
            return Collections.emptyList();
        }

        int pageNumber = Math.max(0, page);
        Pageable pageable = PageRequest.of(pageNumber, size);

        if (SORT_DATE_DESC.equals(normalizedSort)) {
            return answerRepository.findByQuestionIdOrderByCreatedAtDesc(questionId, pageable);
        }
        return answerRepository.findByQuestionIdOrderByUpvotesDescCreatedAtDesc(questionId, pageable);
    }

    @Transactional(readOnly = true)
    public Iterable<Answer> findByUser(UUID userId) {
        return answerRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Iterable<Answer> findByIsVerified(Boolean isVerified) {
        return answerRepository.findByIsVerified(isVerified);
    }

    @Transactional(readOnly = true)
    public Iterable<Answer> findByUserAndIsVerified(UUID userId, Boolean isVerified) {
        return answerRepository.findByUserIdAndIsVerified(userId, isVerified);
    }

    @Transactional(readOnly = true)
    public Iterable<Answer> findByQuestionAndIsVerified(UUID questionId, Boolean isVerified) {
        return answerRepository.findByQuestionIdAndIsVerified(questionId, isVerified);
    }

    @Transactional(readOnly = true)
    public Iterable<Answer> findByQuestionAndUser(UUID questionId, UUID userId) {
        return answerRepository.findByQuestionIdAndUserId(questionId, userId);
    }

    @Transactional(readOnly = true)
    public Iterable<Answer> findByQuestionAndUserAndIsVerified(UUID questionId, UUID userId, Boolean isVerified) {
        return answerRepository.findByQuestionIdAndUserIdAndIsVerified(questionId, userId, isVerified);
    }

    @Transactional
    public Answer updateAnswer(Answer updatedAnswer, UUID answerId, Question question) {
        // 1. Buscamos la respuesta existente en la base de datos
        Answer existingAnswer = findAnswer(answerId);

        // 2. Actualizamos los campos (por ejemplo, el contenido y la ubicación)
        existingAnswer.setContent(updatedAnswer.getContent());
        existingAnswer.setUserLocation(updatedAnswer.getUserLocation());
        validateAnswerLocation(existingAnswer, question);

        // 3. Guardamos y retornamos la respuesta actualizada
        return answerRepository.save(existingAnswer);
    }

    @Transactional
    public Answer updateVotes(UUID answerId, UUID userId, VoteType voteType) {
        Answer answer = findAnswer(answerId);
        RegularUser answerOwner = asRegularUser(answer.getUser());
        if (answerOwner != null && answerOwner.getId() != null && answerOwner.getId().equals(userId)) {
            throw new IllegalArgumentException("Users cannot like or dislike their own answers");
        }

        User voter = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!(voter instanceof RegularUser) && !(voter instanceof BusinessAccount)) {
            throw new AccessDeniedException("Only regular or business users can vote answers");
        }

        Optional<AnswerVote> existingOpt = answerVoteRepository.findByUserIdAndAnswerId(userId, answerId);
        if (existingOpt.isPresent()) {
            AnswerVote existing = existingOpt.get();
            if (existing.getVoteType() == voteType) {
                // Same vote, nothing to do
                return answer;
            }
            // Change vote: swap counters
            if (voteType == VoteType.LIKE) {
                answer.setUpvotes(answer.getUpvotes() + 1);
                answer.setDownvotes(Math.max(0, answer.getDownvotes() - 1));
                decrementDislikes(answerOwner);
                incrementLikes(answerOwner);
            } else {
                answer.setDownvotes(answer.getDownvotes() + 1);
                answer.setUpvotes(Math.max(0, answer.getUpvotes() - 1));
                decrementLikes(answerOwner);
                incrementDislikes(answerOwner);
            }
            existing.setVoteType(voteType);
            existing.setVotedAt(LocalDateTime.now());
            answerVoteRepository.save(existing);
        } else {
            AnswerVote vote = new AnswerVote();
            vote.setAnswer(answer);
            vote.setUser(voter);
            vote.setVoteType(voteType);
            vote.setVotedAt(LocalDateTime.now());
            answerVoteRepository.save(vote);
            if (voteType == VoteType.LIKE) {
                answer.setUpvotes(answer.getUpvotes() + 1);
                incrementLikes(answerOwner);
            } else {
                answer.setDownvotes(answer.getDownvotes() + 1);
                incrementDislikes(answerOwner);
            }
        }

        Answer savedAnswer = answerRepository.save(answer);
        return reconcileAnswerCoins(savedAnswer);
    }

    @Transactional
    public Answer removeVote(UUID answerId, UUID userId) {
        Answer answer = findAnswer(answerId);
        RegularUser answerOwner = asRegularUser(answer.getUser());
        AnswerVote existing = answerVoteRepository.findByUserIdAndAnswerId(userId, answerId)
                .orElseThrow(() -> new IllegalArgumentException("No vote found for this user on this answer"));
        if (existing.getVoteType() == VoteType.LIKE) {
            answer.setUpvotes(Math.max(0, answer.getUpvotes() - 1));
            decrementLikes(answerOwner);
        } else {
            answer.setDownvotes(Math.max(0, answer.getDownvotes() - 1));
            decrementDislikes(answerOwner);
        }
        answerVoteRepository.delete(existing);
        Answer savedAnswer = answerRepository.save(answer);
        return reconcileAnswerCoins(savedAnswer);
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> getUserVotesForQuestion(UUID userId, UUID questionId) {
        List<AnswerVote> votes = answerVoteRepository.findByUserIdAndAnswerQuestionId(userId, questionId);
        return votes.stream().collect(Collectors.toMap(
                v -> v.getAnswer().getId(),
                v -> v.getVoteType().name()));
    }

    @Transactional
    public void deleteAnswer(UUID id) {
        Answer toDelete = findAnswer(id);
        answerRepository.delete(toDelete);
    }

    /**
     * Validates that the answer is posted from a location within the question's
     * radius.
     * Only validates if the question has a location and a radius > 0.
     * 
     * @param answer   The answer to validate
     * @param question The question the answer is replying to
     * @throws IllegalArgumentException if the user location is not within the
     *                                  allowed radius
     */
    private void validateAnswerLocation(Answer answer, Question question) {
        // Event questions are scoped to the event, not a geographic radius.
        if (question.getEvent() != null) {
            return;
        }
        // Only validate location if question has a location and a positive radius
        if (question.getLocation() == null || question.getRadiusKm() == null || question.getRadiusKm() <= 0) {
            // No location validation required
            return;
        }

        // Location validation is required for this question
        if (answer.getUserLocation() == null) {
            throw new IllegalArgumentException("User location must not be null for questions with location radius");
        }

        double distance = calculateDistance(answer.getUserLocation(), question.getLocation());
        double allowedRadius = question.getRadiusKm();

        if (distance > allowedRadius) {
            throw new IllegalArgumentException(
                    String.format("User is not within the allowed radius. Distance: %.2f km, Allowed radius: %.2f km",
                            distance, allowedRadius));
        }
    }

    /**
     * Calculates the distance between two GeoPoints using the Haversine formula.
     * Returns the distance in kilometers.
     * 
     * @param point1 First location
     * @param point2 Second location
     * @return Distance in kilometers
     */
    private double calculateDistance(GeoPoint point1, GeoPoint point2) {
        final int EARTH_RADIUS_KM = 6371;

        double lat1 = Math.toRadians(point1.getLatitude());
        double lon1 = Math.toRadians(point1.getLongitude());
        double lat2 = Math.toRadians(point2.getLatitude());
        double lon2 = Math.toRadians(point2.getLongitude());

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    private void applyDefaults(Answer answer) {
        if (answer.getCreatedAt() == null) {
            answer.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        if (answer.getIsVerified() == null) {
            answer.setIsVerified(false);
        }
        if (answer.getUpvotes() == null) {
            answer.setUpvotes(0);
        }
        if (answer.getDownvotes() == null) {
            answer.setDownvotes(0);
        }
        if (answer.getCoinsEarned() == null) {
            answer.setCoinsEarned(0);
        }
        if (answer.getRewardClaimed() == null) {
            answer.setRewardClaimed(false);
        }
    }

    private Answer reconcileAnswerCoins(Answer answer) {
        if (answer.getUser() == null) {
            return answer;
        }

        int targetCoins = calculateTargetCoinsEarned(answer);
        int currentCoins = answer.getCoinsEarned() == null ? 0 : answer.getCoinsEarned();
        int delta = targetCoins - currentCoins;

        if (delta == 0) {
            return answer;
        }

        RegularUser owner = asRegularUser(answer.getUser());
        if (owner == null) {
            // Business answers are allowed but they don't participate in coin rewards.
            return answer;
        }
        int balanceBefore = owner.getCoinBalance() == null ? 0 : owner.getCoinBalance();
        int balanceAfter = balanceBefore + delta;
        owner.setCoinBalance(balanceAfter);
        regularUserRepository.save(owner);

        answer.setCoinsEarned(targetCoins);
        answer.setRewardClaimed(answer.getUpvotes() != null && answer.getDownvotes() != null
                && !answer.getUpvotes().equals(answer.getDownvotes()));

        CoinTransaction coinTransaction = new CoinTransaction();
        coinTransaction.setUser(owner);
        coinTransaction.setType(delta >= 0 ? CoinTransactionType.EARN : CoinTransactionType.SPEND);
        coinTransaction.setAmount(delta);
        coinTransaction.setCurrency("StreetCoins");
        coinTransaction.setStatus(CoinTransactionStatus.SUCCESS);
        coinTransaction.setBalanceBefore(balanceBefore);
        coinTransaction.setBalanceAfter(balanceAfter);
        coinTransaction.setReferenceId(answer.getId());
        coinTransaction
                .setDescription(delta >= 0 ? "Answer reward" : "Answer reward adjustment");
        coinTransaction.setCreatedAt(LocalDateTime.now());
        coinTransactionRepository.save(coinTransaction);

        return answerRepository.save(answer);
    }

    private int calculateTargetCoinsEarned(Answer answer) {
        if (isSelfAnswer(answer)) {
            return 0;
        }
        if (hasAlreadyAnsweredQuestion(answer)) {
            return 0;
        }
        if (isTooShort(answer)) {
            return 0;
        }

        int likes = answer.getUpvotes() == null ? 0 : answer.getUpvotes();
        int dislikes = answer.getDownvotes() == null ? 0 : answer.getDownvotes();

        if (likes > dislikes) {
            return ANSWER_BASE_REWARD + ANSWER_POSITIVE_VOTE_BONUS;
        }
        if (dislikes > likes) {
            return ANSWER_BASE_REWARD + ANSWER_NEGATIVE_VOTE_PENALTY;
        }
        return ANSWER_BASE_REWARD;
    }

    private boolean isSelfAnswer(Answer answer) {
        if (answer.getUser() == null || answer.getUser().getId() == null) {
            return false;
        }
        if (answer.getQuestion() == null || answer.getQuestion().getCreator() == null
                || answer.getQuestion().getCreator().getId() == null) {
            return false;
        }
        return answer.getUser().getId().equals(answer.getQuestion().getCreator().getId());
    }

    private void applyVerificationStatus(Answer answer) {
        boolean isVerified = isAnswerByEventCreator(answer);

        answer.setIsVerified(isVerified);
        answer.setVerifiedAt(isVerified ? OffsetDateTime.now(ZoneOffset.UTC) : null);
    }

    private boolean isAnswerByEventCreator(Answer answer) {
        if (answer.getUser() == null || answer.getUser().getId() == null) {
            return false;
        }
        if (answer.getQuestion() == null || answer.getQuestion().getEvent() == null
                || answer.getQuestion().getEvent().getCreator() == null
                || answer.getQuestion().getEvent().getCreator().getId() == null) {
            return false;
        }
        return answer.getUser().getId().equals(answer.getQuestion().getEvent().getCreator().getId());
    }

    private boolean hasAlreadyAnsweredQuestion(Answer answer) {
        if (answer.getId() == null || answer.getUser() == null || answer.getUser().getId() == null
                || answer.getQuestion() == null || answer.getQuestion().getId() == null) {
            return false;
        }
        return answerRepository.countByQuestionIdAndUserIdAndIdNot(
                answer.getQuestion().getId(), answer.getUser().getId(), answer.getId()) > 0;
    }

    private boolean isTooShort(Answer answer) {
        String content = answer.getContent();
        return content == null || content.trim().length() < MIN_ANSWER_LENGTH;
    }

    private void validateAnswerRateLimit(Answer answer) {
        if (answer.getUser() == null || answer.getUser().getId() == null) {
            return;
        }
        OffsetDateTime windowStart = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(RATE_LIMIT_WINDOW_MINUTES);
        long recentCount = answerRepository.countByUserIdAndCreatedAtAfter(answer.getUser().getId(), windowStart);
        if (recentCount >= MAX_ANSWERS_IN_WINDOW) {
            throw new IllegalArgumentException(
                    "You are posting too quickly. Please wait before submitting another answer");
        }
    }

    private void attachAuthenticatedUser(Answer answer) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            if (answer.getUser() != null) {
                return;
            }
            throw new AccessDeniedException("Only authenticated users can create answers");
        }

        String identifier = auth.getName().trim();
        User authenticatedUser = userRepository.findByEmailIgnoreCase(identifier)
                .or(() -> userRepository.findByUserNameIgnoreCase(identifier))
                .orElseThrow(() -> new AccessDeniedException("Only regular or business users can create answers"));

        if (!(authenticatedUser instanceof RegularUser) && !(authenticatedUser instanceof BusinessAccount)) {
            throw new AccessDeniedException("Only regular or business users can create answers");
        }

        answer.setUser(authenticatedUser);
    }

    private RegularUser asRegularUser(User user) {
        return user instanceof RegularUser regularUser ? regularUser : null;
    }

    private String normalizeSort(String sort) {
        if (SORT_DATE_DESC.equalsIgnoreCase(sort)) {
            return SORT_DATE_DESC;
        }
        return SORT_LIKES_DESC;
    }

    private void incrementLikes(RegularUser user) {
        if (user == null)
            return;
        int current = user.getTotalLikesReceived() == null ? 0 : user.getTotalLikesReceived();
        user.setTotalLikesReceived(current + 1);
    }

    private void decrementLikes(RegularUser user) {
        if (user == null)
            return;
        int current = user.getTotalLikesReceived() == null ? 0 : user.getTotalLikesReceived();
        user.setTotalLikesReceived(Math.max(0, current - 1));
    }

    private void incrementDislikes(RegularUser user) {
        if (user == null)
            return;
        int current = user.getTotalDislikesReceived() == null ? 0 : user.getTotalDislikesReceived();
        user.setTotalDislikesReceived(current + 1);
    }

    private void decrementDislikes(RegularUser user) {
        if (user == null)
            return;
        int current = user.getTotalDislikesReceived() == null ? 0 : user.getTotalDislikesReceived();
        user.setTotalDislikesReceived(Math.max(0, current - 1));
    }

}
