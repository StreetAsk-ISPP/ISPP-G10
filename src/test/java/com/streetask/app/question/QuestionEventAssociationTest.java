package com.streetask.app.question;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.streetask.app.event.EventRepository;
import com.streetask.app.model.Event;
import com.streetask.app.model.Question;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;
import com.streetask.app.user.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class QuestionEventAssociationTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private RegularUserRepository regularUserRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private QuestionService questionService;

    private UUID eventId;
    private UUID userId;
    private Event testEvent;
    private RegularUser testUser;
    private Question testQuestion;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testUser = new RegularUser();
        testUser.setId(userId);
        testUser.setEmail("testuser@example.com");
        testUser.setUserName("testuser");

        testEvent = new Event();
        testEvent.setId(eventId);
        testEvent.setTitle("Test Event");

        testQuestion = new Question();
        testQuestion.setTitle("Test Question");
        testQuestion.setContent("This is a test question");
        testQuestion.setRadiusKm(0.5f);
        testQuestion.setExpiresAt(null);
        testQuestion.setEvent(new Event());
        testQuestion.getEvent().setId(eventId);

        // Setup authentication
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser@example.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testQuestionCreatedWithEventAssociation() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("testuser@example.com")).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        // Act
        Question savedQuestion = questionService.saveQuestion(testQuestion);

        // Assert
        assertNotNull(savedQuestion.getId(), "Question ID should be set");
        assertNotNull(savedQuestion.getEvent(), "Question event should not be null");
        assertEquals(eventId, savedQuestion.getEvent().getId(), "Question should be associated with the correct event");
        assertEquals("Test Question", savedQuestion.getTitle(), "Question title should match");
        verify(questionRepository, times(1)).save(any(Question.class));
    }

    @Test
    void testMultipleQuestionsRetrievalFromEvent() {
        // This test verifies that when questions are created with event association,
        // they can be retrieved from the event repository
        Question question1 = new Question();
        question1.setId(UUID.randomUUID());
        question1.setTitle("Question 1");
        question1.setEvent(testEvent);

        Question question2 = new Question();
        question2.setId(UUID.randomUUID());
        question2.setTitle("Question 2");
        question2.setEvent(testEvent);

        when(questionRepository.findByEventIdOrderByCreatedAtAsc(eventId))
                .thenReturn(java.util.Arrays.asList(question1, question2));

        // Act
        Iterable<Question> eventQuestions = questionService.findByEvent(eventId);

        // Assert
        java.util.List<Question> questionList = new java.util.ArrayList<>();
        eventQuestions.forEach(questionList::add);
        assertEquals(2, questionList.size(), "Event should have 2 questions");
        assertTrue(questionList.stream().allMatch(q -> q.getEvent().getId().equals(eventId)),
                "All questions should belong to the event");
    }

    @Test
    void testMultipleQuestionsInSameEvent() {
        // Arrange
        UUID question1Id = UUID.randomUUID();
        UUID question2Id = UUID.randomUUID();

        when(userRepository.findByEmailIgnoreCase("testuser@example.com")).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(question1Id);
            }
            return saved;
        });

        // Create first question
        testQuestion.setId(null);
        Question saved1 = questionService.saveQuestion(testQuestion);
        assertEquals(eventId, saved1.getEvent().getId(), "First question should be associated");

        // Create second question
        testQuestion.setId(null);
        testQuestion.setTitle("Second Question");
        Question saved2 = questionService.saveQuestion(testQuestion);
        assertEquals(eventId, saved2.getEvent().getId(), "Second question should be associated");

        // Verify save was called twice
        verify(questionRepository, times(2)).save(any(Question.class));
    }
}
