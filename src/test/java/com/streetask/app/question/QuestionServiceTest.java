package com.streetask.app.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.exceptions.UpperPlanFeatureException;
import com.streetask.app.functionalities.notifications.events.QuestionCreatedEvent;
import com.streetask.app.model.Question;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;

/**
 * Unit tests for QuestionService
 * This tests the service layer logic in isolation using mocks
 */
@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

	@Mock
	private QuestionRepository questionRepository;

	@Mock
	private RegularUserRepository regularUserRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private QuestionService questionService;

	private Question testQuestion;
	private UUID testId;
	private RegularUser testCreator;
	private static final String TEST_EMAIL = "creator@test.com";

	@BeforeEach
	void setUp() {
		testId = UUID.randomUUID();
		testCreator = new RegularUser();
		testCreator.setId(UUID.randomUUID());
		testCreator.setEmail(TEST_EMAIL);
		testCreator.setVisibilityRadiusKm(10.0f);
		testCreator.setPremiumActive(false);

		testQuestion = new Question();
		testQuestion.setId(testId);
		testQuestion.setTitle("Test Question");
		testQuestion.setContent("Test Content");
		testQuestion.setCreator(testCreator);

		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
		Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
		lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
		securityContext.setAuthentication(authentication);
		SecurityContextHolder.setContext(securityContext);
		lenient().when(regularUserRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testCreator));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	// =============== SAVE QUESTION TESTS ===============

	@Test
	void saveQuestion_shouldApplyDefaultsAndSaveSuccessfully() {
		// Arrange
		Question newQuestion = new Question();
		newQuestion.setTitle("New Question");
		newQuestion.setContent("New Content");
		RegularUser otherUser = new RegularUser();
		otherUser.setId(UUID.randomUUID());
		otherUser.setEmail("other@test.com");
		newQuestion.setCreator(otherUser);

		when(questionRepository.save(any(Question.class))).thenReturn(newQuestion);

		// Act
		Question saved = questionService.saveQuestion(newQuestion);

		// Assert
		assertThat(saved.getCreator()).isEqualTo(testCreator);
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getActive()).isTrue();
		assertThat(saved.getAnswerCount()).isZero();
		assertThat(saved.getExpiresAt()).isNotNull();
		assertThat(saved.getExpiresAt()).isAfter(saved.getCreatedAt());
		verify(questionRepository, times(1)).save(newQuestion);
		verify(eventPublisher, times(1)).publishEvent(any(QuestionCreatedEvent.class));
	}

	@Test
	void saveQuestion_shouldSetExpiresAtSixHoursAfterCreatedAt() {
		// Arrange
		Question newQuestion = new Question();
		newQuestion.setTitle("New Question");
		newQuestion.setContent("New Content");

		when(questionRepository.save(any(Question.class))).thenReturn(newQuestion);

		// Act
		Question saved = questionService.saveQuestion(newQuestion);

		// Assert
		assertThat(saved.getExpiresAt()).isEqualTo(saved.getCreatedAt().plus(6, ChronoUnit.HOURS));
	}

	@Test
	void saveQuestion_shouldKeepProvidedCreatedAtActiveAndAnswerCountForFreeUsers() {
		// Arrange
		Instant specificCreatedAt = Instant.now().minus(1, ChronoUnit.DAYS);
		Instant specificExpiresAt = Instant.now().plus(1, ChronoUnit.DAYS);

		Question newQuestion = new Question();
		newQuestion.setTitle("New Question");
		newQuestion.setContent("New Content");
		newQuestion.setCreatedAt(specificCreatedAt);
		newQuestion.setActive(false);
		newQuestion.setAnswerCount(5);
		newQuestion.setExpiresAt(specificExpiresAt);

		when(questionRepository.save(any(Question.class))).thenReturn(newQuestion);

		// Act
		Question saved = questionService.saveQuestion(newQuestion);

		// Assert
		assertThat(saved.getCreatedAt()).isEqualTo(specificCreatedAt);
		assertThat(saved.getActive()).isFalse();
		assertThat(saved.getAnswerCount()).isEqualTo(5);
		assertThat(saved.getExpiresAt()).isNotEqualTo(specificExpiresAt);
		assertThat(saved.getExpiresAt()).isEqualTo(specificCreatedAt.plus(6, ChronoUnit.HOURS));
		assertThat(saved.getCreator()).isEqualTo(testCreator);
	}

	@Test
	void saveQuestion_shouldPublishQuestionCreatedEventWithQuestionId() {
		// Arrange
		Question newQuestion = new Question();
		newQuestion.setId(testId);
		newQuestion.setTitle("New Question");
		newQuestion.setContent("New Content");

		when(questionRepository.save(any(Question.class))).thenReturn(newQuestion);

		// Act
		questionService.saveQuestion(newQuestion);

		// Assert
		ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
		verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
		Object publishedEvent = eventCaptor.getValue();
		assertThat(publishedEvent).isInstanceOf(QuestionCreatedEvent.class);
		QuestionCreatedEvent questionCreatedEvent = (QuestionCreatedEvent) publishedEvent;
		assertThat(questionCreatedEvent.questionId()).isEqualTo(testId);
	}

	@Test
	void saveQuestion_shouldThrowAccessDeniedWhenAuthenticatedUserNotFound() {
		// Arrange
		Question newQuestion = new Question();
		newQuestion.setTitle("New Question");
		newQuestion.setContent("New Content");
		when(regularUserRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> questionService.saveQuestion(newQuestion))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("Only regular users can create questions");
		verify(questionRepository, never()).save(any(Question.class));
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void saveQuestion_shouldForceFreeRadiusToHalfKmWhenRequestedRadiusIsProvided() {
		Question newQuestion = new Question();
		newQuestion.setTitle("Radius test");
		newQuestion.setContent("Should ignore requested radius for free users");
		newQuestion.setRadiusKm(2.0f);

		testCreator.setVisibilityRadiusKm(7.5f);
		when(questionRepository.save(any(Question.class))).thenReturn(newQuestion);

		Question saved = questionService.saveQuestion(newQuestion);

		assertThat(saved.getRadiusKm()).isEqualTo(0.5f);
	}

	@Test
	void saveQuestion_shouldForceFreeRadiusToHalfKmWhenRequestedRadiusIsMissing() {
		Question newQuestion = new Question();
		newQuestion.setTitle("Radius fallback");
		newQuestion.setContent("Should use free fixed radius");
		newQuestion.setRadiusKm(null);

		testCreator.setVisibilityRadiusKm(3.0f);
		when(questionRepository.save(any(Question.class))).thenReturn(newQuestion);

		Question saved = questionService.saveQuestion(newQuestion);

		assertThat(saved.getRadiusKm()).isEqualTo(0.5f);
	}

	@Test
	void saveQuestion_shouldSetDefaultRadiusWhenNoRadiusProvided() {
		Question newQuestion = new Question();
		newQuestion.setTitle("Default radius");
		newQuestion.setContent("Should fallback to 0.5km");
		newQuestion.setRadiusKm(null);

		testCreator.setVisibilityRadiusKm(null);
		when(questionRepository.save(any(Question.class))).thenReturn(newQuestion);

		Question saved = questionService.saveQuestion(newQuestion);

		assertThat(saved.getRadiusKm()).isEqualTo(0.5f);
	}

	@Test
	void saveQuestion_shouldAllowPremiumRadiusAndDurationWithinRange() {
		testCreator.setPremiumActive(true);

		Question newQuestion = new Question();
		newQuestion.setTitle("Premium question");
		newQuestion.setContent("Premium controls");
		newQuestion.setRadiusKm(0.2f);
		newQuestion.setCreatedAt(Instant.now());
		newQuestion.setExpiresAt(newQuestion.getCreatedAt().plus(5, ChronoUnit.HOURS));

		when(questionRepository.save(any(Question.class))).thenReturn(newQuestion);

		Question saved = questionService.saveQuestion(newQuestion);

		assertThat(saved.getRadiusKm()).isEqualTo(0.2f);
		assertThat(saved.getExpiresAt()).isEqualTo(newQuestion.getCreatedAt().plus(5, ChronoUnit.HOURS));
	}

	@Test
	void saveQuestion_shouldAllowPremiumDurationAtOneHourWithSecondDrift() {
		testCreator.setPremiumActive(true);

		Question newQuestion = new Question();
		newQuestion.setTitle("Premium 1h boundary");
		newQuestion.setContent("One hour should be inclusive");
		newQuestion.setRadiusKm(0.2f);
		newQuestion.setCreatedAt(Instant.now());
		newQuestion.setExpiresAt(newQuestion.getCreatedAt().plus(1, ChronoUnit.HOURS).minusSeconds(1));

		when(questionRepository.save(any(Question.class))).thenReturn(newQuestion);

		Question saved = questionService.saveQuestion(newQuestion);

		assertThat(saved.getExpiresAt())
				.isEqualTo(newQuestion.getCreatedAt().plus(1, ChronoUnit.HOURS).minusSeconds(1));
	}

	@Test
	void saveQuestion_shouldRejectPremiumRadiusOutsideAllowedRange() {
		testCreator.setPremiumActive(true);

		Question newQuestion = new Question();
		newQuestion.setTitle("Premium radius");
		newQuestion.setContent("Out of range radius");
		newQuestion.setRadiusKm(2.5f);

		assertThatThrownBy(() -> questionService.saveQuestion(newQuestion))
				.isInstanceOf(UpperPlanFeatureException.class)
				.hasMessageContaining("radius must be between 0.05km and 1km");
	}

	@Test
	void saveQuestion_shouldRejectPremiumDurationOutsideAllowedRange() {
		testCreator.setPremiumActive(true);

		Question newQuestion = new Question();
		newQuestion.setTitle("Premium duration");
		newQuestion.setContent("Out of range duration");
		newQuestion.setRadiusKm(0.5f);
		newQuestion.setCreatedAt(Instant.now());
		newQuestion.setExpiresAt(newQuestion.getCreatedAt().plus(30, ChronoUnit.MINUTES));

		assertThatThrownBy(() -> questionService.saveQuestion(newQuestion))
				.isInstanceOf(UpperPlanFeatureException.class)
				.hasMessageContaining("duration must be between 1h and 24h");
	}

	// =============== FIND QUESTION TESTS ===============

	@Test
	void findQuestion_shouldReturnQuestionWhenExists() {
		// Arrange
		when(questionRepository.findById(testId)).thenReturn(Optional.of(testQuestion));

		// Act
		Question found = questionService.findQuestion(testId);

		// Assert
		assertThat(found).isNotNull();
		assertThat(found.getId()).isEqualTo(testId);
		assertThat(found.getTitle()).isEqualTo("Test Question");
		verify(questionRepository, times(1)).findById(testId);
	}

	@Test
	void findQuestion_shouldThrowResourceNotFoundExceptionWhenDoesNotExist() {
		// Arrange
		UUID nonExistentId = UUID.randomUUID();
		when(questionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> questionService.findQuestion(nonExistentId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Question")
				.hasMessageContaining(nonExistentId.toString());
		verify(questionRepository, times(1)).findById(nonExistentId);
	}

	// =============== FIND ALL TESTS ===============

	@Test
	void findAll_shouldReturnAllQuestions() {
		// Arrange
		Question question2 = new Question();
		question2.setId(UUID.randomUUID());
		question2.setTitle("Question 2");

		when(questionRepository.findAll()).thenReturn(Arrays.asList(testQuestion, question2));

		// Act
		Iterable<Question> questions = questionService.findAll();

		// Assert
		assertThat(questions).hasSize(2);
		verify(questionRepository, times(1)).findAll();
	}

	// =============== FIND BY CREATOR TESTS ===============

	@Test
	void findByCreator_shouldReturnQuestionsForCreator() {
		// Arrange
		UUID creatorId = testCreator.getId();
		when(questionRepository.findByCreatorId(creatorId)).thenReturn(Arrays.asList(testQuestion));

		// Act
		Iterable<Question> questions = questionService.findByCreator(creatorId);

		// Assert
		assertThat(questions).hasSize(1);
		verify(questionRepository, times(1)).findByCreatorId(creatorId);
	}

	// =============== FIND BY EVENT TESTS ===============

	@Test
	void findByEvent_shouldReturnQuestionsForEvent() {
		// Arrange
		UUID eventId = UUID.randomUUID();
		when(questionRepository.findByEventId(eventId)).thenReturn(Arrays.asList(testQuestion));

		// Act
		Iterable<Question> questions = questionService.findByEvent(eventId);

		// Assert
		assertThat(questions).hasSize(1);
		verify(questionRepository, times(1)).findByEventId(eventId);
	}

	// =============== FIND BY ACTIVE TESTS ===============

	@Test
	void findByActive_shouldReturnActiveQuestions() {
		// Arrange
		when(questionRepository.findByActive(true)).thenReturn(Arrays.asList(testQuestion));

		// Act
		Iterable<Question> questions = questionService.findByActive(true);

		// Assert
		assertThat(questions).hasSize(1);
		verify(questionRepository, times(1)).findByActive(true);
	}

	// =============== FIND BY CREATOR AND ACTIVE TESTS ===============

	@Test
	void findByCreatorAndActive_shouldReturnFilteredQuestions() {
		// Arrange
		UUID creatorId = testCreator.getId();
		when(questionRepository.findByCreatorIdAndActive(creatorId, true))
				.thenReturn(Arrays.asList(testQuestion));

		// Act
		Iterable<Question> questions = questionService.findByCreatorAndActive(creatorId, true);

		// Assert
		assertThat(questions).hasSize(1);
		verify(questionRepository, times(1)).findByCreatorIdAndActive(creatorId, true);
	}

	// =============== FIND BY EVENT AND ACTIVE TESTS ===============

	@Test
	void findByEventAndActive_shouldReturnFilteredQuestions() {
		// Arrange
		UUID eventId = UUID.randomUUID();
		when(questionRepository.findByEventIdAndActive(eventId, true))
				.thenReturn(Arrays.asList(testQuestion));

		// Act
		Iterable<Question> questions = questionService.findByEventAndActive(eventId, true);

		// Assert
		assertThat(questions).hasSize(1);
		verify(questionRepository, times(1)).findByEventIdAndActive(eventId, true);
	}

	// =============== FIND BY CREATOR AND EVENT TESTS ===============

	@Test
	void findByCreatorAndEvent_shouldReturnFilteredQuestions() {
		// Arrange
		UUID creatorId = testCreator.getId();
		UUID eventId = UUID.randomUUID();
		when(questionRepository.findByCreatorIdAndEventId(creatorId, eventId))
				.thenReturn(Arrays.asList(testQuestion));

		// Act
		Iterable<Question> questions = questionService.findByCreatorAndEvent(creatorId, eventId);

		// Assert
		assertThat(questions).hasSize(1);
		verify(questionRepository, times(1)).findByCreatorIdAndEventId(creatorId, eventId);
	}

	// =============== FIND BY CREATOR, EVENT AND ACTIVE TESTS ===============

	@Test
	void findByCreatorAndEventAndActive_shouldReturnFilteredQuestions() {
		// Arrange
		UUID creatorId = testCreator.getId();
		UUID eventId = UUID.randomUUID();
		when(questionRepository.findByCreatorIdAndEventIdAndActive(creatorId, eventId, true))
				.thenReturn(Arrays.asList(testQuestion));

		// Act
		Iterable<Question> questions = questionService.findByCreatorAndEventAndActive(creatorId, eventId, true);

		// Assert
		assertThat(questions).hasSize(1);
		verify(questionRepository, times(1)).findByCreatorIdAndEventIdAndActive(creatorId, eventId, true);
	}

	// =============== UPDATE QUESTION TESTS ===============

	@Test
	void updateQuestion_shouldUpdateAndReturnQuestion() {
		// Arrange
		Question updatedData = new Question();
		updatedData.setTitle("Updated Title");
		updatedData.setContent("Updated Content");
		updatedData.setActive(false);

		when(questionRepository.findById(testId)).thenReturn(Optional.of(testQuestion));
		when(questionRepository.save(any(Question.class))).thenReturn(testQuestion);

		// Act
		Question updated = questionService.updateQuestion(updatedData, testId);

		// Assert
		assertThat(updated.getTitle()).isEqualTo("Updated Title");
		assertThat(updated.getContent()).isEqualTo("Updated Content");
		assertThat(updated.getActive()).isFalse();
		verify(questionRepository, times(1)).findById(testId);
		verify(questionRepository, times(1)).save(testQuestion);
	}

	@Test
	void updateQuestion_shouldNotUpdateIdCreatedAtOrAnswerCount() {
		// Arrange
		Instant originalCreatedAt = Instant.now().minus(1, ChronoUnit.DAYS);
		testQuestion.setCreatedAt(originalCreatedAt);
		testQuestion.setAnswerCount(10);

		Question updatedData = new Question();
		UUID differentId = UUID.randomUUID();
		updatedData.setId(differentId);
		updatedData.setTitle("Updated Title");
		updatedData.setCreatedAt(Instant.now());
		updatedData.setAnswerCount(999);

		when(questionRepository.findById(testId)).thenReturn(Optional.of(testQuestion));
		when(questionRepository.save(any(Question.class))).thenReturn(testQuestion);

		// Act
		Question updated = questionService.updateQuestion(updatedData, testId);

		// Assert
		assertThat(updated.getId()).isEqualTo(testId).isNotEqualTo(differentId);
		assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
		assertThat(updated.getAnswerCount()).isEqualTo(10).isNotEqualTo(999);
	}

	@Test
	void updateQuestion_shouldThrowExceptionWhenQuestionNotFound() {
		// Arrange
		UUID nonExistentId = UUID.randomUUID();
		Question updatedData = new Question();
		updatedData.setTitle("Updated Title");

		when(questionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> questionService.updateQuestion(updatedData, nonExistentId))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(questionRepository, times(1)).findById(nonExistentId);
		verify(questionRepository, times(0)).save(any(Question.class));
	}

	// =============== DELETE QUESTION TESTS ===============

	@Test
	void deleteQuestion_shouldDeleteSuccessfully() {
		// Arrange
		when(questionRepository.findById(testId)).thenReturn(Optional.of(testQuestion));

		// Act
		questionService.deleteQuestion(testId);

		// Assert
		verify(questionRepository, times(1)).findById(testId);
		verify(questionRepository, times(1)).delete(testQuestion);
	}

	@Test
	void deleteQuestion_shouldThrowExceptionWhenQuestionNotFound() {
		// Arrange
		UUID nonExistentId = UUID.randomUUID();
		when(questionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> questionService.deleteQuestion(nonExistentId))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(questionRepository, times(1)).findById(nonExistentId);
		verify(questionRepository, times(0)).delete(any(Question.class));
	}

	// =============== EXPIRATION CRON TESTS ===============

	@Test
	void executeExpirationCron_shouldDeactivateExpiredQuestions() {
		// Arrange
		Question expired1 = new Question();
		expired1.setId(UUID.randomUUID());
		expired1.setActive(true);
		expired1.setTitle("Expired 1");

		Question expired2 = new Question();
		expired2.setId(UUID.randomUUID());
		expired2.setActive(true);
		expired2.setTitle("Expired 2");
		when(questionRepository.findAllByActiveTrueAndExpiresAtBefore(any(Instant.class)))
				.thenReturn(Arrays.asList(expired1, expired2));

		// Act
		questionService.executeExpirationCron();

		// Assert
		assertThat(expired1.getActive()).isFalse();
		assertThat(expired2.getActive()).isFalse();
		verify(questionRepository, times(1)).findAllByActiveTrueAndExpiresAtBefore(any(Instant.class));
		verify(questionRepository, times(1)).saveAll(argThat(questions -> {
			if (!(questions instanceof Iterable<?>)) {
				return false;
			}
			int count = 0;
			for (Object ignored : (Iterable<?>) questions) {
				count++;
			}
			return count == 2;
		}));
	}

	@Test
	void executeExpirationCron_shouldDoNothingWhenNoQuestionsExpired() {
		// Arrange
		when(questionRepository.findAllByActiveTrueAndExpiresAtBefore(any(Instant.class)))
				.thenReturn(Arrays.asList());

		// Act
		questionService.executeExpirationCron();

		// Assert
		verify(questionRepository, times(1)).findAllByActiveTrueAndExpiresAtBefore(any(Instant.class));
		verify(questionRepository, never()).saveAll(any());
	}

	// =============== DAILY LIMIT TESTS ===============

	@Test
	void questionsTodayCount_shouldCountOnlyQuestionsFromSameDayCalendar() {
		// Arrange
		UUID userId = testCreator.getId();
		LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
		LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
		
		Question question1 = new Question();
		question1.setCreatedAt(now.minusHours(2)); // 2 hours ago, same day - counts
		
		Question question2 = new Question();
		question2.setCreatedAt(startOfDay.plusMinutes(1)); // Just after midnight, same day - counts
		
		Question question3 = new Question();
		question3.setCreatedAt(startOfDay.minusSeconds(1)); // Just before midnight, previous day - doesn't count
		
		when(questionRepository.findByCreatorId(userId))
				.thenReturn(Arrays.asList(question1, question2, question3));

		// Act
		long count = questionService.questionsTodayCount(userId);

		// Assert
		assertThat(count).isEqualTo(2);
		verify(questionRepository, times(1)).findByCreatorId(userId);
	}

	@Test
	void questionsTodayCount_shouldReturnZeroWhenNoQuestionsCreated() {
		// Arrange
		UUID userId = testCreator.getId();
		when(questionRepository.findByCreatorId(userId)).thenReturn(Arrays.asList());

		// Act
		long count = questionService.questionsTodayCount(userId);

		// Assert
		assertThat(count).isZero();
		verify(questionRepository, times(1)).findByCreatorId(userId);
	}

	@Test
	void getTodayQuestionCountForAuthenticatedUser_shouldReturnCountForAuthenticatedUser() {
		// Arrange
		UUID userId = testCreator.getId();
		LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
		
		Question question1 = new Question();
		question1.setCreatedAt(now.minusHours(1));
		
		Question question2 = new Question();
		question2.setCreatedAt(now.minusHours(5));
		
		when(questionRepository.findByCreatorId(userId))
				.thenReturn(Arrays.asList(question1, question2));

		// Act
		long count = questionService.getTodayQuestionCountForAuthenticatedUser();

		// Assert
		assertThat(count).isEqualTo(2);
		verify(regularUserRepository, times(1)).findByEmail(TEST_EMAIL);
		verify(questionRepository, times(1)).findByCreatorId(userId);
	}

	@Test
	void getTodayQuestionCountForAuthenticatedUser_shouldThrowExceptionWhenUserNotFound() {
		// Arrange
		when(regularUserRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> questionService.getTodayQuestionCountForAuthenticatedUser())
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("Only regular users can check their question count");
	}

	@Test
	void saveQuestion_shouldRejectFreeUserAfter3QuestionsPerDay() {
		// Arrange
		testCreator.setPremiumActive(false);
		UUID userId = testCreator.getId();
		LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
		
		// Mock 3 existing questions created today
		Question question1 = new Question();
		question1.setCreatedAt(now.minusHours(1));
		
		Question question2 = new Question();
		question2.setCreatedAt(now.minusHours(4));
		
		Question question3 = new Question();
		question3.setCreatedAt(now.minusHours(8));
		
		when(questionRepository.findByCreatorId(userId))
				.thenReturn(Arrays.asList(question1, question2, question3));

		Question newQuestion = new Question();
		newQuestion.setTitle("Fourth Question");
		newQuestion.setContent("This should fail");

		// Act & Assert
		assertThatThrownBy(() -> questionService.saveQuestion(newQuestion))
				.isInstanceOf(UpperPlanFeatureException.class)
				.hasMessageContaining("Free plan users can only create up to 3 questions");
		
		verify(questionRepository, never()).save(any(Question.class));
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void saveQuestion_shouldAllowFreeUserToCreateUpTo3QuestionsPerDay() {
		// Arrange
		testCreator.setPremiumActive(false);
		UUID userId = testCreator.getId();
		LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
		
		// Mock 2 existing questions created today
		Question question1 = new Question();
		question1.setCreatedAt(now.minusHours(2));
		
		Question question2 = new Question();
		question2.setCreatedAt(now.minusHours(6));
		
		when(questionRepository.findByCreatorId(userId))
				.thenReturn(Arrays.asList(question1, question2));

		Question newQuestion = new Question();
		newQuestion.setTitle("Third Question");
		newQuestion.setContent("This should succeed");
		newQuestion.setRadiusKm(0.5f);

		when(questionRepository.save(any(Question.class))).thenReturn(newQuestion);

		// Act
		Question saved = questionService.saveQuestion(newQuestion);

		// Assert
		assertThat(saved).isNotNull();
		verify(questionRepository, times(1)).save(newQuestion);
		verify(eventPublisher, times(1)).publishEvent(any(QuestionCreatedEvent.class));
	}

	@Test
	void saveQuestion_shouldNotEnforceDailyLimitForPremiumUsers() {
		// Arrange
		testCreator.setPremiumActive(true);
		UUID userId = testCreator.getId();
		LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
		
		// Mock 5 existing questions created today (way over the free limit)
		// Note: This mock is not used for premium users since daily limit is not enforced
		Question[] questions = new Question[5];
		for (int i = 0; i < 5; i++) {
			questions[i] = new Question();
			questions[i].setCreatedAt(now.minusHours(i + 1));
		}
		
		lenient().when(questionRepository.findByCreatorId(userId))
				.thenReturn(Arrays.asList(questions));

		Question newQuestion = new Question();
		newQuestion.setTitle("Sixth Premium Question");
		newQuestion.setContent("Premium users have no daily limit");
		newQuestion.setRadiusKm(0.2f);

		when(questionRepository.save(any(Question.class))).thenReturn(newQuestion);

		// Act
		Question saved = questionService.saveQuestion(newQuestion);

		// Assert
		assertThat(saved).isNotNull();
		verify(questionRepository, times(1)).save(newQuestion);
		verify(eventPublisher, times(1)).publishEvent(any(QuestionCreatedEvent.class));
	}

}
