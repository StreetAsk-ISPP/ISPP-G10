package com.streetask.app.functionalities.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;

@ExtendWith(MockitoExtension.class)
class FeedbackMessageServiceTest {

    @Mock
    private FeedbackMessageRepository feedbackMessageRepository;

    @Mock
    private RegularUserRepository regularUserRepository;

    @InjectMocks
    private FeedbackMessageService feedbackMessageService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createFeedback_shouldTrimMessageAndAttachUserByEmail() {
        RegularUser user = new RegularUser();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setUserName("tester");

        FeedbackMessageRequest request = new FeedbackMessageRequest();
        request.setMessage("  hello feedback  ");
        request.setType(FeedbackType.BUG);

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user@test.com", null, List.of()));

        when(regularUserRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(feedbackMessageRepository.save(any(FeedbackMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackMessage saved = feedbackMessageService.createFeedback(request);

        assertEquals("hello feedback", saved.getMessage());
        assertEquals(FeedbackType.BUG, saved.getType());
        assertEquals("tester", saved.getUserName());
        assertEquals(user, saved.getUser());
    }

    @Test
    void createFeedback_shouldResolveUserByUsernameWhenEmailNotFound() {
        RegularUser user = new RegularUser();
        user.setUserName("tester");

        FeedbackMessageRequest request = new FeedbackMessageRequest();
        request.setMessage("hello");
        request.setType(FeedbackType.OTHER);

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("tester", null, List.of()));

        when(regularUserRepository.findByEmail("tester")).thenReturn(Optional.empty());
        when(regularUserRepository.findByUserNameIgnoreCase("tester")).thenReturn(Optional.of(user));
        when(feedbackMessageRepository.save(any(FeedbackMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackMessage saved = feedbackMessageService.createFeedback(request);

        assertEquals("tester", saved.getUserName());
    }

    @Test
    void createFeedback_shouldThrowWhenUnauthenticated() {
        FeedbackMessageRequest request = new FeedbackMessageRequest();
        request.setMessage("hello");
        request.setType(FeedbackType.SUGGESTION);

        assertThrows(AccessDeniedException.class, () -> feedbackMessageService.createFeedback(request));
        verify(feedbackMessageRepository, never()).save(any());
    }

    @Test
    void createFeedback_shouldThrowWhenAuthenticatedNameIsBlank() {
        FeedbackMessageRequest request = new FeedbackMessageRequest();
        request.setMessage("hello");
        request.setType(FeedbackType.SUGGESTION);

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("   ", null, List.of()));

        assertThrows(AccessDeniedException.class, () -> feedbackMessageService.createFeedback(request));
    }

    @Test
    void createFeedback_shouldThrowWhenAuthenticatedNameIsNull() {
        FeedbackMessageRequest request = new FeedbackMessageRequest();
        request.setMessage("hello");
        request.setType(FeedbackType.SUGGESTION);

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThrows(AccessDeniedException.class, () -> feedbackMessageService.createFeedback(request));
    }

    @Test
    void createFeedback_shouldThrowWhenRegularUserNotFound() {
        FeedbackMessageRequest request = new FeedbackMessageRequest();
        request.setMessage("hello");
        request.setType(FeedbackType.SUGGESTION);

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("missing", null, List.of()));

        when(regularUserRepository.findByEmail("missing")).thenReturn(Optional.empty());
        when(regularUserRepository.findByUserNameIgnoreCase("missing")).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> feedbackMessageService.createFeedback(request));
    }

    @Test
    void findAllOrderedByCreatedAtDesc_shouldDelegate() {
        when(feedbackMessageRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(new FeedbackMessage()));

        Iterable<FeedbackMessage> result = feedbackMessageService.findAllOrderedByCreatedAtDesc();
        int count = 0;
        for (FeedbackMessage ignored : result) {
            count++;
        }
        assertEquals(1, count);
    }

    @Test
    void findById_shouldReturnEntity() {
        UUID id = UUID.randomUUID();
        FeedbackMessage message = new FeedbackMessage();
        when(feedbackMessageRepository.findById(id)).thenReturn(Optional.of(message));

        FeedbackMessage result = feedbackMessageService.findById(id);

        assertEquals(message, result);
    }

    @Test
    void findById_shouldThrowWhenMissing() {
        UUID id = UUID.randomUUID();
        when(feedbackMessageRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> feedbackMessageService.findById(id));
    }

    @Test
    void deleteById_shouldDeleteEntity() {
        UUID id = UUID.randomUUID();
        FeedbackMessage message = new FeedbackMessage();
        when(feedbackMessageRepository.findById(id)).thenReturn(Optional.of(message));

        feedbackMessageService.deleteById(id);

        verify(feedbackMessageRepository).delete(message);
    }

    @Test
    void deleteById_shouldThrowWhenMissing() {
        UUID id = UUID.randomUUID();
        when(feedbackMessageRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> feedbackMessageService.deleteById(id));
    }

    @Test
    void feedbackMessage_prePersist_shouldSetCreatedAt() {
        FeedbackMessage message = new FeedbackMessage();

        message.prePersist();

        assertNotNull(message.getCreatedAt());
    }
}
