package com.streetask.app.question;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streetask.app.event.EventRepository;
import com.streetask.app.business.BusinessAccount;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.exceptions.UpperPlanFeatureException;
import com.streetask.app.functionalities.notifications.events.QuestionCreatedEvent;
import com.streetask.app.model.Event;
import com.streetask.app.model.Question;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;
import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class QuestionService {

	private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);
	private static final float FREE_FIXED_RADIUS_KM = 0.5f;
	private static final float PREMIUM_MIN_RADIUS_KM = 0.05f;
	private static final float PREMIUM_MAX_RADIUS_KM = 1.0f;
	private static final int FREE_DURATION_HOURS = 6;
	private static final int PREMIUM_MIN_DURATION_HOURS = 1;
	private static final int PREMIUM_MAX_DURATION_HOURS = 24;
	private static final long PREMIUM_DURATION_CLOCK_DRIFT_SECONDS = 59L;
	private static final int FREE_DAILY_LIMIT = 3;
	private static final int FREE_LIMIT_ROLLING_WINDOW_HOURS = 24;

	private final QuestionRepository questionRepository;
	private final RegularUserRepository regularUserRepository;
	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Autowired
	public QuestionService(
			QuestionRepository questionRepository,
			RegularUserRepository regularUserRepository,
			UserRepository userRepository,
			EventRepository eventRepository,
			ApplicationEventPublisher eventPublisher) {
		this.questionRepository = questionRepository;
		this.regularUserRepository = regularUserRepository;
		this.userRepository = userRepository;
		this.eventRepository = eventRepository;
		this.eventPublisher = eventPublisher;
	}

	public long questionsTodayCount(UUID creatorId) {
		Instant startOfDay = startOfTodayUtc();
		return StreamSupport.stream(questionRepository.findByCreatorId(creatorId).spliterator(), false)
				.filter(q -> q.getCreatedAt() != null && !q.getCreatedAt().isBefore(startOfDay))
				.count();
	}

	public long questionsTodayCountByEvent(UUID creatorId, UUID eventId) {
		Instant startOfDay = startOfTodayUtc();
		return StreamSupport
				.stream(questionRepository.findByCreatorIdAndEventId(creatorId, eventId).spliterator(), false)
				.filter(q -> q.getCreatedAt() != null && !q.getCreatedAt().isBefore(startOfDay))
				.count();
	}

	@Transactional(readOnly = true)
	public long getTodayQuestionCountForAuthenticatedUser(UUID eventId) {
		User user = getAuthenticatedUser();

		if (eventId != null) {
			return questionsTodayCountByEvent(user.getId(), eventId);
		}

		return questionsTodayCount(user.getId());
	}

	@Transactional
	public Question saveQuestion(@Valid Question question) throws DataAccessException {
		User authenticatedUser = getAuthenticatedUser();

		UUID eventId = question.getEvent() != null ? question.getEvent().getId() : null;
		logger.info("[QuestionService] saveQuestion - title='{}', eventId='{}'", question.getTitle(), eventId);

		if (eventId != null) {
			if (!(authenticatedUser instanceof RegularUser) && !(authenticatedUser instanceof BusinessAccount)) {
				throw new AccessDeniedException("Only regular or business users can create event questions");
			}

			Event event = eventRepository.findById(eventId)
					.orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));
			question.setEvent(event);
			logger.info("[QuestionService] Event set for question: {}", eventId);

			long todayCountForEvent = questionsTodayCountByEvent(authenticatedUser.getId(), eventId);
			if (todayCountForEvent >= 3) {
				throw new UpperPlanFeatureException(
						"A user can create a maximum of 3 questions per day in the same event.");
			}
		} else if (!(authenticatedUser instanceof RegularUser)) {
			throw new AccessDeniedException("Only regular users can create questions outside events");
		}

		boolean isPremium = hasPremiumAccess(authenticatedUser);
		if (!isPremium && eventId == null) {
			long todayQuestionCount = questionsTodayCount(authenticatedUser.getId());
			long rollingWindowQuestionCount = questionsCountInRollingHours(authenticatedUser.getId(),
					FREE_LIMIT_ROLLING_WINDOW_HOURS);
			if (todayQuestionCount >= FREE_DAILY_LIMIT || rollingWindowQuestionCount >= FREE_DAILY_LIMIT) {
				throw new UpperPlanFeatureException("Free plan users can only create up to 3 questions.");
			}
		}
		question.setCreator(authenticatedUser);
		question.setRadiusKm(resolveRadiusKm(question.getRadiusKm(), isPremium));
		applyDefaults(question, isPremium);
		Question savedQuestion = questionRepository.save(question);
		logger.info("[QuestionService] Question saved: id='{}', eventId='{}', title='{}'",
				savedQuestion.getId(),
				savedQuestion.getEvent() != null ? savedQuestion.getEvent().getId() : "null",
				savedQuestion.getTitle());
		eventPublisher.publishEvent(new QuestionCreatedEvent(question.getId()));
		return savedQuestion;
	}

	@Transactional(readOnly = true)
	public Question findQuestion(UUID id) {
		return questionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Question", "id", id));
	}

	@Transactional(readOnly = true)
	public Iterable<Question> findAll() {
		return questionRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Iterable<Question> findByCreator(UUID creatorId) {
		return questionRepository.findByCreatorId(creatorId);
	}

	@Transactional(readOnly = true)
	public Iterable<Question> findByEvent(UUID eventId) {
		logger.info("[QuestionService] findByEvent called for eventId: {}", eventId);
		java.util.List<Question> questions = StreamSupport
				.stream(questionRepository.findByEventIdOrderByCreatedAtAsc(eventId).spliterator(), false)
				.toList();
		logger.info("[QuestionService] Found {} questions for event: {}", questions.size(), eventId);
		return questions;
	}

	@Transactional(readOnly = true)
	public Iterable<Question> findByActive(Boolean active) {
		return questionRepository.findByActive(active);
	}

	@Transactional(readOnly = true)
	public Iterable<Question> findByCreatorAndActive(UUID creatorId, Boolean active) {
		return questionRepository.findByCreatorIdAndActive(creatorId, active);
	}

	@Transactional(readOnly = true)
	public Iterable<Question> findByEventAndActive(UUID eventId, Boolean active) {
		return questionRepository.findByEventIdAndActive(eventId, active);
	}

	@Transactional(readOnly = true)
	public Iterable<Question> findByCreatorAndEvent(UUID creatorId, UUID eventId) {
		return questionRepository.findByCreatorIdAndEventId(creatorId, eventId);
	}

	@Transactional(readOnly = true)
	public Iterable<Question> findByCreatorAndEventAndActive(UUID creatorId, UUID eventId, Boolean active) {
		return questionRepository.findByCreatorIdAndEventIdAndActive(creatorId, eventId, active);
	}

	@Transactional
	public Question updateQuestion(@Valid Question question, UUID idToUpdate) {
		Question toUpdate = findQuestion(idToUpdate);
		BeanUtils.copyProperties(question, toUpdate, "id", "creator", "createdAt", "answerCount");
		boolean isPremium = toUpdate.getCreator() != null && hasPremiumAccess(toUpdate.getCreator());
		applyDefaults(toUpdate, isPremium);
		questionRepository.save(toUpdate);
		return toUpdate;
	}

	@Transactional
	public void deleteQuestion(UUID id) {
		Question toDelete = findQuestion(id);
		questionRepository.delete(toDelete);
	}

	@Transactional
	@Scheduled(cron = "0 * * * * *")
	public void executeExpirationCron() {
		Instant now = Instant.now();
		Iterable<Question> expiredQuestions = questionRepository.findAllByActiveTrueAndExpiresAtBefore(now);

		if (expiredQuestions.iterator().hasNext()) {
			expiredQuestions.forEach(question -> {
				question.setActive(false);
			});
			questionRepository.saveAll(expiredQuestions);
		}
	}

	private void applyDefaults(Question question, boolean isPremium) {
		if (question.getCreatedAt() == null) {
			question.setCreatedAt(Instant.now());
		}
		if (question.getActive() == null) {
			question.setActive(true);
		}
		if (question.getAnswerCount() == null) {
			question.setAnswerCount(0);
		}
		if (question.getExpiresAt() == null) {
			question.setExpiresAt(question.getCreatedAt().plus(FREE_DURATION_HOURS, ChronoUnit.HOURS));
		}
		if (!isPremium) {
			question.setExpiresAt(question.getCreatedAt().plus(FREE_DURATION_HOURS, ChronoUnit.HOURS));
			return;
		}

		long durationSeconds = Duration.between(question.getCreatedAt(), question.getExpiresAt()).toSeconds();
		long minDurationSeconds = (PREMIUM_MIN_DURATION_HOURS * 3600L) - PREMIUM_DURATION_CLOCK_DRIFT_SECONDS;
		long maxDurationSeconds = PREMIUM_MAX_DURATION_HOURS * 3600L;
		if (durationSeconds < minDurationSeconds || durationSeconds > maxDurationSeconds) {
			throw new UpperPlanFeatureException("Premium question duration must be between 1h and 24h.");
		}
	}

	private Float resolveRadiusKm(Float requestedRadiusKm, boolean isPremium) {
		if (!isPremium) {
			return FREE_FIXED_RADIUS_KM;
		}

		if (requestedRadiusKm == null) {
			return FREE_FIXED_RADIUS_KM;
		}

		if (requestedRadiusKm < PREMIUM_MIN_RADIUS_KM || requestedRadiusKm > PREMIUM_MAX_RADIUS_KM) {
			throw new UpperPlanFeatureException("Premium question radius must be between 0.05km and 1km.");
		}

		return requestedRadiusKm;
	}

	private Instant startOfTodayUtc() {
		return Instant.now().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
	}

	private User getAuthenticatedUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
			throw new AccessDeniedException("Authenticated user required");
		}

		String identifier = auth.getName().trim();
		return userRepository.findByEmailIgnoreCase(identifier)
				.or(() -> userRepository.findByUserNameIgnoreCase(identifier))
				.or(() -> findByUuid(identifier))
				.orElseThrow(() -> new AccessDeniedException("Authenticated user required"));
	}

	private java.util.Optional<User> findByUuid(String identifier) {
		try {
			UUID id = UUID.fromString(identifier);
			return userRepository.findById(id);
		} catch (IllegalArgumentException ex) {
			return java.util.Optional.empty();
		}
	}

	private boolean hasPremiumAccess(User user) {
		if (user instanceof RegularUser regularUser) {
			return Boolean.TRUE.equals(regularUser.getPremiumActive());
		}
		if (user instanceof BusinessAccount businessAccount) {
			return Boolean.TRUE.equals(businessAccount.getPremiumActive());
		}
		return false;
	}

	private long questionsCountInRollingHours(UUID creatorId, int hours) {
		Instant windowStart = Instant.now().minus(hours, ChronoUnit.HOURS);
		return StreamSupport.stream(questionRepository.findByCreatorId(creatorId).spliterator(), false)
				.filter(q -> q.getCreatedAt() != null && !q.getCreatedAt().isBefore(windowStart))
				.count();
	}
}
