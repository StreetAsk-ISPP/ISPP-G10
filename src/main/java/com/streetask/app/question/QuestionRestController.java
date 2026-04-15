package com.streetask.app.question;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.streetask.app.auth.payload.response.MessageResponse;
import com.streetask.app.model.Event;
import com.streetask.app.model.Question;
import com.streetask.app.util.RestPreconditions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/questions")
@SecurityRequirement(name = "bearerAuth")
public class QuestionRestController {

	private static final Logger logger = LoggerFactory.getLogger(QuestionRestController.class);
	private final QuestionService questionService;

	@Autowired
	public QuestionRestController(QuestionService questionService) {
		this.questionService = questionService;
	}

	@GetMapping
	public ResponseEntity<Iterable<Question>> findAll(@RequestParam(required = false) UUID creatorId,
			@RequestParam(required = false) UUID eventId, @RequestParam(required = false) Boolean active) {
		Iterable<Question> res;
		if (creatorId != null && eventId != null && active != null) {
			res = questionService.findByCreatorAndEventAndActive(creatorId, eventId, active);
		} else if (creatorId != null && eventId != null) {
			res = questionService.findByCreatorAndEvent(creatorId, eventId);
		} else if (creatorId != null && active != null) {
			res = questionService.findByCreatorAndActive(creatorId, active);
		} else if (eventId != null && active != null) {
			res = questionService.findByEventAndActive(eventId, active);
		} else if (creatorId != null) {
			res = questionService.findByCreator(creatorId);
		} else if (eventId != null) {
			res = questionService.findByEvent(eventId);
		} else if (active != null) {
			res = questionService.findByActive(active);
		} else {
			res = questionService.findAll();
		}
		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	@GetMapping(value = "{id}")
	public ResponseEntity<Question> findById(@PathVariable("id") UUID id) {
		return new ResponseEntity<>(questionService.findQuestion(id), HttpStatus.OK);
	}

	@GetMapping("/today-count")
	public ResponseEntity<Long> getTodayQuestionCount(@RequestParam(required = false) UUID eventId) {
		long count = questionService.getTodayQuestionCountForAuthenticatedUser(eventId);
		return new ResponseEntity<>(count, HttpStatus.OK);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<Question> create(@RequestBody @Valid CreateQuestionRequest request) {
		Question question = new Question();
		question.setTitle(request.getTitle());
		question.setContent(request.getContent());
		question.setLocation(request.getLocation());
		question.setRadiusKm(request.getRadiusKm());
		question.setExpiresAt(request.getExpiresAt());
		if (request.getEvent() != null && request.getEvent().getId() != null) {
			Event eventRef = new Event();
			eventRef.setId(request.getEvent().getId());
			question.setEvent(eventRef);
		}

		logger.info("[QuestionRestController] Creating question: title='{}', eventId='{}'",
				question.getTitle(),
				question.getEvent() != null ? question.getEvent().getId() : "null");
		Question savedQuestion = questionService.saveQuestion(question, request.getConfirmStreetCoinSpend());
		logger.info("[QuestionRestController] Question saved with id={}, eventId={}",
				savedQuestion.getId(),
				savedQuestion.getEvent() != null ? savedQuestion.getEvent().getId() : "null");
		return new ResponseEntity<>(savedQuestion, HttpStatus.CREATED);
	}

	@PutMapping(value = "{questionId}")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<Question> update(@PathVariable("questionId") UUID id,
			@RequestBody @Valid Question question) {
		RestPreconditions.checkNotNull(questionService.findQuestion(id), "Question", "ID", id);
		return new ResponseEntity<>(this.questionService.updateQuestion(question, id), HttpStatus.OK);
	}

	@DeleteMapping(value = "{questionId}")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<MessageResponse> delete(@PathVariable("questionId") UUID id) {
		RestPreconditions.checkNotNull(questionService.findQuestion(id), "Question", "ID", id);
		questionService.deleteQuestion(id);
		return new ResponseEntity<>(new MessageResponse("Question deleted!"), HttpStatus.OK);
	}

}
