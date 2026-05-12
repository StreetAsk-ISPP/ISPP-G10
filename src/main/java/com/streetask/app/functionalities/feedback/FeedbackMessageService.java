package com.streetask.app.functionalities.feedback;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;

@Service
public class FeedbackMessageService {

    private final FeedbackMessageRepository feedbackMessageRepository;
    private final RegularUserRepository regularUserRepository;

    @Autowired
    public FeedbackMessageService(
            FeedbackMessageRepository feedbackMessageRepository,
            RegularUserRepository regularUserRepository) {
        this.feedbackMessageRepository = feedbackMessageRepository;
        this.regularUserRepository = regularUserRepository;
    }

    @Transactional
    public FeedbackMessage createFeedback(FeedbackMessageRequest request) {
        FeedbackMessage feedbackMessage = new FeedbackMessage();
        feedbackMessage.setMessage(request.getMessage().trim());
        feedbackMessage.setType(request.getType());

        attachAuthenticatedUser(feedbackMessage);

        return feedbackMessageRepository.save(feedbackMessage);
    }

    @Transactional(readOnly = true)
    public Iterable<FeedbackMessage> findAllOrderedByCreatedAtDesc() {
        return feedbackMessageRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public FeedbackMessage findById(@org.springframework.lang.NonNull UUID id) {
        return feedbackMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feedback message not found with id: " + id));
    }

    @Transactional
    public void deleteById(@org.springframework.lang.NonNull UUID id) {
        FeedbackMessage feedbackMessage = feedbackMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feedback message not found with id: " + id));

        feedbackMessageRepository.delete(feedbackMessage);
    }

    private void attachAuthenticatedUser(FeedbackMessage feedbackMessage) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new AccessDeniedException("Only authenticated regular users can send feedback");
        }

        String identifier = auth.getName().trim();

        RegularUser regularUser = regularUserRepository.findByEmail(identifier)
                .or(() -> regularUserRepository.findByUserNameIgnoreCase(identifier))
                .orElseThrow(() -> new AccessDeniedException("Only regular users can send feedback"));

        feedbackMessage.setUser(regularUser);
        feedbackMessage.setUserName(regularUser.getUserName());
    }
}