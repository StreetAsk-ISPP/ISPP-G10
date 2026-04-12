package com.streetask.app.answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.functionalities.notifications.events.AnswerCreatedEvent;
import com.streetask.app.model.Answer;
import com.streetask.app.model.AnswerVote;
import com.streetask.app.model.CoinTransactionRepository;
import com.streetask.app.model.GeoPoint;
import com.streetask.app.model.Question;
import com.streetask.app.model.enums.VoteType;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;

class AnswerServiceTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AnswerVoteRepository answerVoteRepository;

    @Mock
    private RegularUserRepository regularUserRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CoinTransactionRepository coinTransactionRepository;

    @InjectMocks
    private AnswerService answerService;

    private Answer answer;
    private Question question;
    private RegularUser authenticatedUser;
    private RegularUser answerOwner;
    private RegularUser regularUser;
    private UUID answerId;
    private UUID questionId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        answerId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        userId = UUID.randomUUID();

        authenticatedUser = new RegularUser();
        authenticatedUser.setId(userId);
        authenticatedUser.setEmail("testuser@example.com");
        authenticatedUser.setUserName("testuser");

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(authentication.getName()).thenReturn(authenticatedUser.getEmail());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(regularUserRepository.findByEmail(authenticatedUser.getEmail()))
                .thenReturn(Optional.of(authenticatedUser));
        when(regularUserRepository.findByUserNameIgnoreCase(authenticatedUser.getEmail()))
                .thenReturn(Optional.empty());

        answerOwner = new RegularUser();
        answerOwner.setId(UUID.randomUUID());
        answerOwner.setCoinBalance(0);

        // Create test question with location and radius
        question = new Question();
        question.setId(questionId);
        question.setTitle("Test Question");
        question.setContent("Test Question Content");
        question.setLocation(new GeoPoint());
        question.getLocation().setLatitude(37.7749); // San Francisco
        question.getLocation().setLongitude(-122.4194);
        question.setRadiusKm(1.0f);
        question.setActive(true);
        question.setCreatedAt(Instant.now());

        // Create test answer
        answer = new Answer();
        answer.setId(answerId);
        answer.setQuestion(question);
        answer.setContent("Test Answer");
        answer.setUser(answerOwner);
        answer.setUserLocation(new GeoPoint());
        answer.getUserLocation().setLatitude(37.7749); // Same location as question
        answer.getUserLocation().setLongitude(-122.4194);
        answer.setUpvotes(0);
        answer.setDownvotes(0);
        answer.setRewardClaimed(false);
        answer.setCoinsEarned(0);

        regularUser = new RegularUser();
        regularUser.setId(userId);
        regularUser.setCreatedAt(LocalDateTime.now().minusDays(60));
        regularUser.setActive(true);

        when(regularUserRepository.findById(userId)).thenReturn(Optional.of(regularUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSaveAnswerWithValidLocation() {
        when(answerRepository.save(answer)).thenReturn(answer);

        Answer savedAnswer = answerService.saveAnswer(answer, question);

        assertNotNull(savedAnswer);
        assertEquals(answerId, savedAnswer.getId());
        assertEquals("Test Answer", savedAnswer.getContent());
        assertNotNull(savedAnswer.getCreatedAt());
        assertEquals(false, savedAnswer.getIsVerified());
        assertEquals(0, savedAnswer.getUpvotes());
        assertEquals(0, savedAnswer.getDownvotes());
        assertEquals(1, savedAnswer.getCoinsEarned());
        assertEquals(1, authenticatedUser.getCoinBalance());

        verify(answerRepository, times(2)).save(answer);
        verify(coinTransactionRepository, times(1)).save(any());

        ArgumentCaptor<AnswerCreatedEvent> eventCaptor = ArgumentCaptor.forClass(AnswerCreatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertEquals(answerId, eventCaptor.getValue().answerId());
    }

    @Test
    void testSaveAnswerByQuestionCreatorEarnsNoCoins() {
        // Set question creator to the authenticated user — this is a self-answer
        question.setCreator(authenticatedUser);
        authenticatedUser.setCoinBalance(5);

        when(answerRepository.save(answer)).thenReturn(answer);

        Answer savedAnswer = answerService.saveAnswer(answer, question);

        assertEquals(0, savedAnswer.getCoinsEarned());
        assertEquals(5, authenticatedUser.getCoinBalance());
        verify(coinTransactionRepository, never()).save(any());
        verify(answerRepository, times(1)).save(answer);
    }

    @Test
    void testSaveAnswerByDifferentUserEarnsCoins() {
        // Question creator is a different user — reward should be granted
        RegularUser questionCreator = new RegularUser();
        questionCreator.setId(UUID.randomUUID());
        question.setCreator(questionCreator);
        authenticatedUser.setCoinBalance(0);

        when(answerRepository.save(answer)).thenReturn(answer);

        Answer savedAnswer = answerService.saveAnswer(answer, question);

        assertEquals(1, savedAnswer.getCoinsEarned());
        assertEquals(1, authenticatedUser.getCoinBalance());
        verify(coinTransactionRepository, times(1)).save(any());
    }

    @Test
    void testSaveAnswerWithTooShortContentSavesButEarnsNoCoins() {
        answer.setContent("Short");
        authenticatedUser.setCoinBalance(0);
        when(answerRepository.save(answer)).thenReturn(answer);

        Answer savedAnswer = answerService.saveAnswer(answer, question);

        assertEquals(0, savedAnswer.getCoinsEarned());
        assertEquals(0, authenticatedUser.getCoinBalance());
        verify(coinTransactionRepository, never()).save(any());
        verify(answerRepository, times(1)).save(answer);
    }

    @Test
    void testSaveAnswerRateLimitExceededThrows() {
        when(answerRepository.countByUserIdAndCreatedAtAfter(eq(userId), any(OffsetDateTime.class)))
                .thenReturn(5L);

        assertThrows(IllegalArgumentException.class, () -> answerService.saveAnswer(answer, question));

        verify(answerRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testSaveAnswerDuplicateToSameQuestionEarnsNoCoins() {
        // User already has another answer to the same question
        when(answerRepository.countByQuestionIdAndUserIdAndIdNot(questionId, userId, answerId))
                .thenReturn(1L);
        when(answerRepository.save(answer)).thenReturn(answer);

        Answer savedAnswer = answerService.saveAnswer(answer, question);

        assertEquals(0, savedAnswer.getCoinsEarned());
        verify(coinTransactionRepository, never()).save(any());
        verify(answerRepository, times(1)).save(answer);
    }

    @Test
    void testSaveAnswerOutsideRadius() {
        // Move answer location far away
        answer.getUserLocation().setLatitude(37.8044); // About 3.2 km away
        answer.getUserLocation().setLongitude(-122.2712);

        assertThrows(IllegalArgumentException.class, () -> {
            answerService.saveAnswer(answer, question);
        });

        verify(answerRepository, never()).save(answer);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testSaveAnswerWithNullLocation() {
        answer.setUserLocation(null);

        assertThrows(IllegalArgumentException.class, () -> {
            answerService.saveAnswer(answer, question);
        });

        verify(answerRepository, never()).save(answer);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testSaveAnswerWhenQuestionHasNoLocation() {
        answer.setUserLocation(null);
        answer.setCoinsEarned(null);
        question.setLocation(null);

        when(answerRepository.save(answer)).thenReturn(answer);

        Answer savedAnswer = answerService.saveAnswer(answer, question);

        assertNotNull(savedAnswer);
        verify(answerRepository, times(2)).save(answer);
        verify(eventPublisher, times(1)).publishEvent(any(AnswerCreatedEvent.class));
    }

    @Test
    void testSaveAnswerWhenQuestionHasNullRadius() {
        answer.setUserLocation(null);
        answer.setCoinsEarned(null);
        question.setRadiusKm(null);

        when(answerRepository.save(answer)).thenReturn(answer);

        Answer savedAnswer = answerService.saveAnswer(answer, question);

        assertNotNull(savedAnswer);
        verify(answerRepository, times(2)).save(answer);
        verify(eventPublisher, times(1)).publishEvent(any(AnswerCreatedEvent.class));
    }

    @Test
    void testSaveAnswerWhenQuestionHasZeroRadius() {
        answer.setUserLocation(null);
        answer.setCoinsEarned(null);
        question.setRadiusKm(0f);

        when(answerRepository.save(answer)).thenReturn(answer);

        Answer savedAnswer = answerService.saveAnswer(answer, question);

        assertNotNull(savedAnswer);
        verify(answerRepository, times(2)).save(answer);
        verify(eventPublisher, times(1)).publishEvent(any(AnswerCreatedEvent.class));
    }

    @Test
    void testSaveAnswerWhenQuestionHasNegativeRadius() {
        answer.setUserLocation(null);
        answer.setCoinsEarned(null);
        question.setRadiusKm(-2f);

        when(answerRepository.save(answer)).thenReturn(answer);

        Answer savedAnswer = answerService.saveAnswer(answer, question);

        assertNotNull(savedAnswer);
        verify(answerRepository, times(2)).save(answer);
        verify(eventPublisher, times(1)).publishEvent(any(AnswerCreatedEvent.class));
    }

    @Test
    void testFindAnswerById() {
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));

        Answer foundAnswer = answerService.findAnswer(answerId);

        assertNotNull(foundAnswer);
        assertEquals(answerId, foundAnswer.getId());
        verify(answerRepository, times(1)).findById(answerId);
    }

    @Test
    void testFindAnswerByIdNotFound() {
        when(answerRepository.findById(answerId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            answerService.findAnswer(answerId);
        });

        assertTrue(exception.getMessage().contains("Answer not found with id"));
        verify(answerRepository, times(1)).findById(answerId);
    }

    @Test
    void testFindAllDelegatesToRepository() {
        List<Answer> answers = List.of(answer);
        when(answerRepository.findAll()).thenReturn(answers);

        Iterable<Answer> result = answerService.findAll();

        assertSame(answers, result);
        verify(answerRepository, times(1)).findAll();
    }

    @Test
    void testFindByQuestionDelegatesToRepository() {
        List<Answer> answers = List.of(answer);
        when(answerRepository.findByQuestionId(questionId)).thenReturn(answers);

        Iterable<Answer> result = answerService.findByQuestion(questionId);

        assertSame(answers, result);
        verify(answerRepository, times(1)).findByQuestionId(questionId);
    }

    @Test
    void testFindByQuestionSortedDefaultsToTopWhenSortIsNull() {
        List<Answer> answers = List.of(answer);
        when(answerRepository.findByQuestionIdOrderByUpvotesDescCreatedAtDesc(questionId)).thenReturn(answers);

        List<Answer> result = answerService.findByQuestionSorted(questionId, null, null, null);

        assertSame(answers, result);
        verify(answerRepository, times(1)).findByQuestionIdOrderByUpvotesDescCreatedAtDesc(questionId);
        verify(answerRepository, never()).findByQuestionIdOrderByCreatedAtDesc(questionId);
    }

    @Test
    void testFindByQuestionSortedDateDescWithoutPagination() {
        List<Answer> answers = List.of(answer);
        when(answerRepository.findByQuestionIdOrderByCreatedAtDesc(questionId)).thenReturn(answers);

        List<Answer> result = answerService.findByQuestionSorted(questionId, "date_desc", null, null);

        assertSame(answers, result);
        verify(answerRepository, times(1)).findByQuestionIdOrderByCreatedAtDesc(questionId);
        verify(answerRepository, never()).findByQuestionIdOrderByUpvotesDescCreatedAtDesc(questionId);
    }

    @Test
    void testFindByQuestionSortedDefaultsToTopForInvalidSort() {
        List<Answer> answers = List.of(answer);
        when(answerRepository.findByQuestionIdOrderByUpvotesDescCreatedAtDesc(questionId)).thenReturn(answers);

        List<Answer> result = answerService.findByQuestionSorted(questionId, "invalid_sort", null, null);

        assertSame(answers, result);
        verify(answerRepository, times(1)).findByQuestionIdOrderByUpvotesDescCreatedAtDesc(questionId);
    }

    @Test
    void testFindByQuestionSortedWithPaginationUsesTopOrder() {
        List<Answer> answers = List.of(answer);
        when(answerRepository.findByQuestionIdOrderByUpvotesDescCreatedAtDesc(eq(questionId), any()))
                .thenReturn(answers);

        List<Answer> result = answerService.findByQuestionSorted(questionId, "likes_desc", 0, 10);

        assertSame(answers, result);
        verify(answerRepository, times(1)).findByQuestionIdOrderByUpvotesDescCreatedAtDesc(eq(questionId), any());
        verify(answerRepository, never()).findByQuestionIdOrderByCreatedAtDesc(eq(questionId), any());
    }

    @Test
    void testFindByQuestionSortedWithPaginationUsesDateOrder() {
        List<Answer> answers = List.of(answer);
        when(answerRepository.findByQuestionIdOrderByCreatedAtDesc(eq(questionId), any()))
                .thenReturn(answers);

        List<Answer> result = answerService.findByQuestionSorted(questionId, "date_desc", 1, 5);

        assertSame(answers, result);
        verify(answerRepository, times(1)).findByQuestionIdOrderByCreatedAtDesc(eq(questionId), any());
        verify(answerRepository, never()).findByQuestionIdOrderByUpvotesDescCreatedAtDesc(eq(questionId), any());
    }

    @Test
    void testFindByQuestionSortedWithInvalidSizeReturnsEmpty() {
        List<Answer> result = answerService.findByQuestionSorted(questionId, "likes_desc", 0, 0);

        assertTrue(result.isEmpty());
        verify(answerRepository, never()).findByQuestionIdOrderByUpvotesDescCreatedAtDesc(eq(questionId), any());
        verify(answerRepository, never()).findByQuestionIdOrderByCreatedAtDesc(eq(questionId), any());
    }

    @Test
    void testFindByUserDelegatesToRepository() {
        List<Answer> answers = List.of(answer);
        when(answerRepository.findByUserId(userId)).thenReturn(answers);

        Iterable<Answer> result = answerService.findByUser(userId);

        assertSame(answers, result);
        verify(answerRepository, times(1)).findByUserId(userId);
    }

    @Test
    void testFindByIsVerifiedDelegatesToRepository() {
        List<Answer> answers = List.of(answer);
        when(answerRepository.findByIsVerified(true)).thenReturn(answers);

        Iterable<Answer> result = answerService.findByIsVerified(true);

        assertSame(answers, result);
        verify(answerRepository, times(1)).findByIsVerified(true);
    }

    @Test
    void testFindByUserAndIsVerifiedDelegatesToRepository() {
        List<Answer> answers = List.of(answer);
        when(answerRepository.findByUserIdAndIsVerified(userId, true)).thenReturn(answers);

        Iterable<Answer> result = answerService.findByUserAndIsVerified(userId, true);

        assertSame(answers, result);
        verify(answerRepository, times(1)).findByUserIdAndIsVerified(userId, true);
    }

    @Test
    void testFindByQuestionAndIsVerifiedDelegatesToRepository() {
        List<Answer> answers = List.of(answer);
        when(answerRepository.findByQuestionIdAndIsVerified(questionId, true)).thenReturn(answers);

        Iterable<Answer> result = answerService.findByQuestionAndIsVerified(questionId, true);

        assertSame(answers, result);
        verify(answerRepository, times(1)).findByQuestionIdAndIsVerified(questionId, true);
    }

    @Test
    void testFindByQuestionAndUserDelegatesToRepository() {
        List<Answer> answers = List.of(answer);
        when(answerRepository.findByQuestionIdAndUserId(questionId, userId)).thenReturn(answers);

        Iterable<Answer> result = answerService.findByQuestionAndUser(questionId, userId);

        assertSame(answers, result);
        verify(answerRepository, times(1)).findByQuestionIdAndUserId(questionId, userId);
    }

    @Test
    void testFindByQuestionAndUserAndIsVerifiedDelegatesToRepository() {
        List<Answer> answers = List.of(answer);
        when(answerRepository.findByQuestionIdAndUserIdAndIsVerified(questionId, userId, true)).thenReturn(answers);

        Iterable<Answer> result = answerService.findByQuestionAndUserAndIsVerified(questionId, userId, true);

        assertSame(answers, result);
        verify(answerRepository, times(1)).findByQuestionIdAndUserIdAndIsVerified(questionId, userId, true);
    }

    @Test
    void testDeleteAnswer() {
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));

        answerService.deleteAnswer(answerId);

        verify(answerRepository, times(1)).findById(answerId);
        verify(answerRepository, times(1)).delete(answer);
    }

    @Test
    void testUpdateAnswerWithValidLocation() {
        Answer updatedAnswer = new Answer();
        updatedAnswer.setContent("Updated Answer");
        updatedAnswer.setUserLocation(new GeoPoint());
        updatedAnswer.getUserLocation().setLatitude(37.7749);
        updatedAnswer.getUserLocation().setLongitude(-122.4194);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(answerRepository.save(answer)).thenReturn(answer);

        Answer result = answerService.updateAnswer(updatedAnswer, answerId, question);

        assertNotNull(result);
        assertSame(answer, result);
        assertEquals("Updated Answer", result.getContent());
        assertEquals(37.7749, result.getUserLocation().getLatitude());
        assertEquals(-122.4194, result.getUserLocation().getLongitude());
        verify(answerRepository, times(1)).findById(answerId);
        verify(answerRepository, times(1)).save(answer);
    }

    @Test
    void testUpdateVotesNewLikeVote() {
        answer.setUpvotes(2);
        answer.setDownvotes(3);
        answer.setCoinsEarned(1);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(answerVoteRepository.findByUserIdAndAnswerId(userId, answerId)).thenReturn(Optional.empty());
        when(answerRepository.save(answer)).thenReturn(answer);

        Answer result = answerService.updateVotes(answerId, userId, VoteType.LIKE);

        assertEquals(3, result.getUpvotes());
        assertEquals(3, result.getDownvotes());
        verify(answerVoteRepository, times(1)).save(any(AnswerVote.class));
        verify(answerRepository, atLeastOnce()).save(answer);
    }

    @Test
    void testUpdateVotesNewDislikeVote() {
        answer.setUpvotes(2);
        answer.setDownvotes(3);
        answer.setCoinsEarned(1);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(answerVoteRepository.findByUserIdAndAnswerId(userId, answerId)).thenReturn(Optional.empty());
        when(answerRepository.save(answer)).thenReturn(answer);

        Answer result = answerService.updateVotes(answerId, userId, VoteType.DISLIKE);

        assertEquals(2, result.getUpvotes());
        assertEquals(4, result.getDownvotes());
        verify(answerVoteRepository, times(1)).save(any(AnswerVote.class));
        verify(answerRepository, atLeastOnce()).save(answer);
    }

    @Test
    void testUpdateVotesSameVoteIsNoOp() {
        AnswerVote existing = new AnswerVote();
        existing.setVoteType(VoteType.LIKE);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(answerVoteRepository.findByUserIdAndAnswerId(userId, answerId)).thenReturn(Optional.of(existing));

        answerService.updateVotes(answerId, userId, VoteType.LIKE);

        verify(answerRepository, never()).save(any(Answer.class));
        verify(answerVoteRepository, never()).save(any(AnswerVote.class));
    }

    @Test
    void testUpdateVotesChangeLikeToDislike() {
        answer.setUpvotes(2);
        answer.setDownvotes(1);
        answer.setCoinsEarned(2);

        AnswerVote existing = new AnswerVote();
        existing.setVoteType(VoteType.LIKE);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(answerVoteRepository.findByUserIdAndAnswerId(userId, answerId)).thenReturn(Optional.of(existing));
        when(answerRepository.save(answer)).thenReturn(answer);

        Answer result = answerService.updateVotes(answerId, userId, VoteType.DISLIKE);

        assertEquals(1, result.getUpvotes());
        assertEquals(2, result.getDownvotes());
        assertEquals(VoteType.DISLIKE, existing.getVoteType());
        verify(answerVoteRepository, times(1)).save(existing);
        verify(answerRepository, atLeastOnce()).save(answer);
    }

    @Test
    void testUpdateVotesChangeDislikeToLike() {
        answer.setUpvotes(1);
        answer.setDownvotes(2);
        answer.setCoinsEarned(0);

        AnswerVote existing = new AnswerVote();
        existing.setVoteType(VoteType.DISLIKE);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(answerVoteRepository.findByUserIdAndAnswerId(userId, answerId)).thenReturn(Optional.of(existing));
        when(answerRepository.save(answer)).thenReturn(answer);

        Answer result = answerService.updateVotes(answerId, userId, VoteType.LIKE);

        assertEquals(2, result.getUpvotes());
        assertEquals(1, result.getDownvotes());
        assertEquals(VoteType.LIKE, existing.getVoteType());
        verify(answerVoteRepository, times(1)).save(existing);
        verify(answerRepository, atLeastOnce()).save(answer);
    }

    @Test
    void testUpdateVotesNotFound() {
        when(answerRepository.findById(answerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> answerService.updateVotes(answerId, userId, VoteType.LIKE));

        verify(answerRepository, times(1)).findById(answerId);
        verify(answerRepository, never()).save(any(Answer.class));
    }

    @Test
    void testUpdateVotesRejectsSelfVote() {
        answer.setUser(authenticatedUser);
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));

        assertThrows(IllegalArgumentException.class,
                () -> answerService.updateVotes(answerId, userId, VoteType.LIKE));

        verify(answerVoteRepository, never()).save(any(AnswerVote.class));
        verify(answerRepository, never()).save(any(Answer.class));
    }

    @Test
    void testUpdateVotesGivesAdditionalCoinWhenLikesExceedDislikes() {
        answer.setUpvotes(0);
        answer.setDownvotes(0);
        answer.setCoinsEarned(1);
        answerOwner.setCoinBalance(7);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(answerVoteRepository.findByUserIdAndAnswerId(userId, answerId)).thenReturn(Optional.empty());
        when(answerRepository.save(answer)).thenReturn(answer);

        Answer result = answerService.updateVotes(answerId, userId, VoteType.LIKE);

        assertEquals(1, result.getUpvotes());
        assertTrue(result.getRewardClaimed());
        assertEquals(8, answerOwner.getCoinBalance());
        assertEquals(2, result.getCoinsEarned());
        verify(coinTransactionRepository, times(1)).save(any());
        verify(answerRepository, times(2)).save(answer);
    }

    @Test
    void testUpdateVotesDoesNotChangeCoinsWhenVoteKeepsSameState() {
        answer.setUpvotes(2);
        answer.setDownvotes(0);
        answer.setCoinsEarned(2);
        answer.setRewardClaimed(true);
        answerOwner.setCoinBalance(10);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(answerVoteRepository.findByUserIdAndAnswerId(userId, answerId)).thenReturn(Optional.empty());
        when(answerRepository.save(answer)).thenReturn(answer);

        answerService.updateVotes(answerId, userId, VoteType.LIKE);

        assertEquals(10, answerOwner.getCoinBalance());
        verify(coinTransactionRepository, never()).save(any());
    }

    @Test
    void testRemoveVoteLike() {
        answer.setUpvotes(3);
        answer.setDownvotes(1);
        answer.setCoinsEarned(2);

        AnswerVote existing = new AnswerVote();
        existing.setAnswer(answer);
        existing.setVoteType(VoteType.LIKE);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(answerVoteRepository.findByUserIdAndAnswerId(userId, answerId)).thenReturn(Optional.of(existing));
        when(answerRepository.save(answer)).thenReturn(answer);

        Answer result = answerService.removeVote(answerId, userId);

        assertEquals(2, result.getUpvotes());
        assertEquals(1, result.getDownvotes());
        verify(answerVoteRepository, times(1)).delete(existing);
        verify(answerRepository, atLeastOnce()).save(answer);
    }

    @Test
    void testRemoveVoteDislike() {
        answer.setUpvotes(1);
        answer.setDownvotes(3);
        answer.setCoinsEarned(0);

        AnswerVote existing = new AnswerVote();
        existing.setAnswer(answer);
        existing.setVoteType(VoteType.DISLIKE);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(answerVoteRepository.findByUserIdAndAnswerId(userId, answerId)).thenReturn(Optional.of(existing));
        when(answerRepository.save(answer)).thenReturn(answer);

        Answer result = answerService.removeVote(answerId, userId);

        assertEquals(1, result.getUpvotes());
        assertEquals(2, result.getDownvotes());
        verify(answerVoteRepository, times(1)).delete(existing);
        verify(answerRepository, atLeastOnce()).save(answer);
    }

    @Test
    void testRemoveVoteNotFound() {
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(answerVoteRepository.findByUserIdAndAnswerId(userId, answerId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> answerService.removeVote(answerId, userId));

        verify(answerVoteRepository, never()).delete(any(AnswerVote.class));
        verify(answerRepository, never()).save(any(Answer.class));
    }

    @Test
    void testGetUserVotesForQuestion() {
        AnswerVote likeVote = new AnswerVote();
        likeVote.setAnswer(answer);
        likeVote.setVoteType(VoteType.LIKE);

        when(answerVoteRepository.findByUserIdAndAnswerQuestionId(userId, questionId))
                .thenReturn(List.of(likeVote));

        Map<UUID, String> result = answerService.getUserVotesForQuestion(userId, questionId);

        assertEquals(1, result.size());
        assertEquals("LIKE", result.get(answerId));
        verify(answerVoteRepository, times(1)).findByUserIdAndAnswerQuestionId(userId, questionId);
    }
}
