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

import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.exceptions.UpperPlanFeatureException;
import com.streetask.app.functionalities.notifications.events.QuestionCreatedEvent;
import com.streetask.app.model.Question;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;

import jakarta.validation.Valid;

@Service
public class QuestionService {

	private static final float FREE_FIXED_RADIUS_KM = 0.5f;
	private static final float PREMIUM_MIN_RADIUS_KM = 0.05f;
	private static final float PREMIUM_MAX_RADIUS_KM = 1.0f;
	private static final int FREE_DURATION_HOURS = 6;
	private static final int PREMIUM_MIN_DURATION_HOURS = 1;
	private static final int PREMIUM_MAX_DURATION_HOURS = 24;
	private static final long PREMIUM_DURATION_CLOCK_DRIFT_SECONDS = 59L;

	private final QuestionRepository questionRepository;
	private final RegularUserRepository regularUserRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Autowired
	public QuestionService(
			QuestionRepository questionRepository,
			RegularUserRepository regularUserRepository,
			ApplicationEventPublisher eventPublisher) {
		this.questionRepository = questionRepository;
		this.regularUserRepository = regularUserRepository;
		this.eventPublisher = eventPublisher;
	}
	public long questionsTodayCount(UUID creatorId) {
		Instant now = Instant.now();
		Instant startOfDay = now.atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
		return StreamSupport.stream(questionRepository.findByCreatorId(creatorId).spliterator(), false)
				.filter(q -> q.getCreatedAt() != null && q.getCreatedAt().isAfter(startOfDay))
				.count();
	}

	@Transactional(readOnly = true)
	public long getTodayQuestionCountForAuthenticatedUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String email = auth.getName();
		
		RegularUser ru = regularUserRepository.findByEmail(email)
				.orElseThrow(() -> new AccessDeniedException("Only regular users can check their question count"));
		
		return questionsTodayCount(ru.getId());
	}

	@Transactional
	public Question saveQuestion(@Valid Question question) throws DataAccessException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String email = auth.getName();

		RegularUser ru = regularUserRepository.findByEmail(email)
				.orElseThrow(() -> new AccessDeniedException("Only regular users can create questions"));
		boolean isPremium = Boolean.TRUE.equals(ru.getPremiumActive());
		if (!isPremium) {
			long userQuestionCount = questionRepository.countByCreatorId(ru.getId());
			long todayQuestionCount = questionsTodayCount(ru.getId());
			if (todayQuestionCount >= 3) {
				throw new UpperPlanFeatureException("Free plan users can only create up to 3 questions.");
			}
		}
		question.setCreator(ru);
		question.setRadiusKm(resolveRadiusKm(question.getRadiusKm(), isPremium));
		applyDefaults(question, isPremium);
		questionRepository.save(question);
		eventPublisher.publishEvent(new QuestionCreatedEvent(question.getId()));
		return question;
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
		return questionRepository.findByEventId(eventId);
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
		BeanUtils.copyProperties(question, toUpdate, "id", "createdAt", "answerCount");
		boolean isPremium = toUpdate.getCreator() != null
				&& Boolean.TRUE.equals(toUpdate.getCreator().getPremiumActive());
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
}
