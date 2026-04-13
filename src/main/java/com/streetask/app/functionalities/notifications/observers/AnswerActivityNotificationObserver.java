package com.streetask.app.functionalities.notifications.observers;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.streetask.app.answer.AnswerRepository;
import com.streetask.app.functionalities.notifications.events.AnswerCreatedEvent;
import com.streetask.app.functionalities.notifications.push.dto.PushMessage;
import com.streetask.app.functionalities.notifications.push.service.PushNotificationService;
import com.streetask.app.functionalities.notifications.realtime.FrontendNotificationGateway;
import com.streetask.app.functionalities.notifications.realtime.FrontendNotificationMessage;
import com.streetask.app.model.Answer;
import com.streetask.app.model.Question;
import com.streetask.app.question.QuestionRepository;
import com.streetask.app.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnswerActivityNotificationObserver {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final FrontendNotificationGateway frontendNotificationGateway;
    private final PushNotificationService pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnswerCreated(AnswerCreatedEvent event) {
        Answer answer = answerRepository.findById(event.answerId()).orElse(null);
        if (answer == null || answer.getQuestion() == null) {
            return;
        }

        Question answerQuestion = answer.getQuestion();
        Question question = questionRepository.findById(answerQuestion.getId()).orElse(answerQuestion);
        User actor = answer.getUser();
        String actorEmail = actor != null ? actor.getEmail() : null;
        String actorName = actor != null && actor.getUserName() != null ? actor.getUserName() : "Someone";

        Set<String> recipientEmails = new LinkedHashSet<>();
        if (question.getCreator() != null && question.getCreator().getEmail() != null) {
            recipientEmails.add(question.getCreator().getEmail());
        }

        Iterable<Answer> questionAnswers = answerRepository.findByQuestionId(question.getId());
        for (Answer existingAnswer : questionAnswers) {
            if (existingAnswer.getUser() != null && existingAnswer.getUser().getEmail() != null) {
                recipientEmails.add(existingAnswer.getUser().getEmail());
            }
        }

        if (actorEmail != null) {
            recipientEmails.remove(actorEmail);
        }

        if (recipientEmails.isEmpty()) {
            log.info("Skipping ANSWER_TO_QUESTION notification: no recipients. questionId={} actorEmail={}",
                    question.getId(), actorEmail);
            return;
        }

        FrontendNotificationMessage payload = FrontendNotificationMessage.builder()
                .type("ANSWER_TO_QUESTION")
                .title("New activity on a followed question")
                .message(actorName + " answered: " + answer.getContent())
                .referenceId(question.getId())
                .referenceType("QUESTION")
                .timestamp(LocalDateTime.now())
                .build();

        PushMessage pushMessage = PushMessage.builder()
                .title("New activity on a followed question")
                .body(actorName + " answered: " + answer.getContent())
                .type("ANSWER_TO_QUESTION")
                .referenceId(question.getId())
                .referenceType("QUESTION")
                .build();

        for (String recipientEmail : recipientEmails) {
            frontendNotificationGateway.sendToUser(recipientEmail, payload);
            pushNotificationService.sendToUser(recipientEmail, pushMessage);
        }

        log.info("Published ANSWER_TO_QUESTION notification. questionId={} actorEmail={} recipients={}",
                question.getId(), actorEmail, recipientEmails);
    }
}
