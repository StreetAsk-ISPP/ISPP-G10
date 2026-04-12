package com.streetask.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetask.app.model.Question;
import com.streetask.app.question.QuestionRepository;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.UserRepository;

import jakarta.transaction.Transactional;

/**
 * Integration tests for QuestionRestController
 * This tests the REST endpoints with real Spring context
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuestionRestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private RegularUser testCreator;
    private Question testQuestion;

    @BeforeEach
    void setUp() {
        // Find existing authority from data.sql
        Authorities userAuthority = entityManager.find(Authorities.class,
                java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"));

        // Create a test user
        testCreator = new RegularUser();
        testCreator.setEmail("testcreator@streetask.com");
        testCreator.setUserName("testcreator");
        testCreator.setFirstName("Test");
        testCreator.setLastName("Creator");
        testCreator.setVisibilityRadiusKm(10.0f);
        testCreator.setPremiumActive(false);
        testCreator.setAuthority(userAuthority);
        testCreator = userRepository.save(testCreator);

        // Create a test question
        testQuestion = new Question();
        testQuestion.setTitle("Test Question");
        testQuestion.setContent("Test Content");
        testQuestion.setCreator(testCreator);
        testQuestion.setActive(true);
        testQuestion.setAnswerCount(0);
        testQuestion.setCreatedAt(Instant.now());
        testQuestion.setExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS));
        testQuestion = questionRepository.save(testQuestion);
    }

    // =============== GET ALL QUESTIONS TESTS ===============

    @Test
    @WithMockUser
    void findAll_shouldReturnAllQuestions() throws Exception {
        mockMvc.perform(get("/api/v1/questions")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[?(@.title == 'Test Question')].content",
                        org.hamcrest.Matchers.hasItem("Test Content")));
    }

    @Test
    @WithMockUser
    void findAll_withCreatorIdParam_shouldReturnFilteredQuestions() throws Exception {
        mockMvc.perform(get("/api/v1/questions")
                .param("creatorId", testCreator.getId().toString())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Test Question"));
    }

    @Test
    @WithMockUser
    void findAll_withActiveParam_shouldReturnFilteredQuestions() throws Exception {
        mockMvc.perform(get("/api/v1/questions")
                .param("active", "true")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @WithMockUser
    void findAll_withMultipleParams_shouldReturnFilteredQuestions() throws Exception {
        mockMvc.perform(get("/api/v1/questions")
                .param("creatorId", testCreator.getId().toString())
                .param("active", "true")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithMockUser
    void findAll_withEventIdParam_shouldReturnFilteredQuestions() throws Exception {
        UUID eventId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/questions")
                .param("eventId", eventId.toString())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    void findAll_withEventIdAndActiveParams_shouldReturnFilteredQuestions() throws Exception {
        UUID eventId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/questions")
                .param("eventId", eventId.toString())
                .param("active", "true")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    void findAll_withCreatorIdAndEventIdParams_shouldReturnFilteredQuestions() throws Exception {
        UUID eventId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/questions")
                .param("creatorId", testCreator.getId().toString())
                .param("eventId", eventId.toString())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    void findAll_withCreatorIdEventIdAndActiveParams_shouldReturnFilteredQuestions() throws Exception {
        UUID eventId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/questions")
                .param("creatorId", testCreator.getId().toString())
                .param("eventId", eventId.toString())
                .param("active", "true")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =============== GET QUESTION BY ID TESTS ===============

    @Test
    @WithMockUser
    void findById_shouldReturnQuestionWhenExists() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", testQuestion.getId())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testQuestion.getId().toString()))
                .andExpect(jsonPath("$.title").value("Test Question"))
                .andExpect(jsonPath("$.content").value("Test Content"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser
    void findById_shouldReturnNotFoundWhenQuestionDoesNotExist() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/questions/{id}", nonExistentId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // =============== CREATE QUESTION TESTS ===============

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldCreateQuestionWhenPayloadIsValid() throws Exception {
        Map<String, Object> questionPayload = createValidQuestionPayload();

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Question"))
                .andExpect(jsonPath("$.content").value("New Content"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.answerCount").value(0))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.expiresAt").exists());

        // Verify the question was actually saved
        assertThat(questionRepository.count()).isEqualTo(6);
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldApplyDefaultValues() throws Exception {
        Map<String, Object> questionPayload = new HashMap<>();
        questionPayload.put("title", "Question Without Defaults");
        questionPayload.put("content", "Content");
        questionPayload.put("creator", Map.of("id", testCreator.getId().toString()));

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.answerCount").value(0))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldForceFreeRadiusToHalfKmEvenWhenPayloadRadiusIsProvided() throws Exception {
        Map<String, Object> questionPayload = createValidQuestionPayload();
        questionPayload.put("radiusKm", 2.0);

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.radiusKm").value(0.5));
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldAllowPremiumRadiusInRange() throws Exception {
        testCreator.setPremiumActive(true);
        userRepository.save(testCreator);

        Map<String, Object> questionPayload = createValidQuestionPayload();
        questionPayload.put("radiusKm", 0.2);
        questionPayload.put("expiresAt", LocalDateTime.now().plusHours(3).toString());

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.radiusKm").value(0.2));
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldRejectPremiumRadiusOutOfRange() throws Exception {
        testCreator.setPremiumActive(true);
        userRepository.save(testCreator);

        Map<String, Object> questionPayload = createValidQuestionPayload();
        questionPayload.put("radiusKm", 2.0);
        questionPayload.put("expiresAt", LocalDateTime.now().plusHours(3).toString());

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldAcceptDateTimeWithoutTimezone() throws Exception {
        Map<String, Object> questionPayload = createValidQuestionPayload();
        questionPayload.put("expiresAt", "2026-03-02T17:37:32");

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldReturnBadRequestWhenTitleIsMissing() throws Exception {
        Map<String, Object> invalidPayload = new HashMap<>();
        invalidPayload.put("content", "Content without title");
        invalidPayload.put("creator", Map.of("id", testCreator.getId().toString()));

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldReturnBadRequestWhenContentIsMissing() throws Exception {
        Map<String, Object> invalidPayload = new HashMap<>();
        invalidPayload.put("title", "Title without content");
        invalidPayload.put("creator", Map.of("id", testCreator.getId().toString()));

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    // =============== UPDATE QUESTION TESTS ===============

    @Test
    @WithMockUser
    void update_shouldUpdateQuestionWhenExists() throws Exception {
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("title", "Updated Title");
        updatePayload.put("content", "Updated Content");
        updatePayload.put("active", false);
        updatePayload.put("creator", Map.of("id", testCreator.getId().toString()));

        mockMvc.perform(put("/api/v1/questions/{questionId}", testQuestion.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testQuestion.getId().toString()))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser
    void update_shouldNotUpdateAnswerCount() throws Exception {
        // Set initial answer count
        testQuestion.setAnswerCount(5);
        questionRepository.save(testQuestion);

        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("title", "Updated Title");
        updatePayload.put("content", "Updated Content");
        updatePayload.put("answerCount", 999); // Try to change it
        updatePayload.put("creator", Map.of("id", testCreator.getId().toString()));

        mockMvc.perform(put("/api/v1/questions/{questionId}", testQuestion.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerCount").value(5)); // Should remain unchanged
    }

    @Test
    @WithMockUser
    void update_shouldReturnNotFoundWhenQuestionDoesNotExist() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        Map<String, Object> updatePayload = createValidQuestionPayload();

        mockMvc.perform(put("/api/v1/questions/{questionId}", nonExistentId)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void update_shouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        Map<String, Object> invalidPayload = new HashMap<>();
        invalidPayload.put("title", ""); // Invalid: empty title
        invalidPayload.put("creator", Map.of("id", testCreator.getId().toString()));

        mockMvc.perform(put("/api/v1/questions/{questionId}", testQuestion.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    // =============== DELETE QUESTION TESTS ===============

    @Test
    @WithMockUser
    void delete_shouldDeleteQuestionWhenExists() throws Exception {
        mockMvc.perform(delete("/api/v1/questions/{questionId}", testQuestion.getId())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Question deleted!"));

        // Verify the question was actually deleted
        assertThat(questionRepository.findById(testQuestion.getId())).isEmpty();
    }

    @Test
    @WithMockUser
    void delete_shouldReturnNotFoundWhenQuestionDoesNotExist() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/questions/{questionId}", nonExistentId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // =============== HELPER METHODS ===============

    private Map<String, Object> createValidQuestionPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "New Question");
        payload.put("content", "New Content");
        payload.put("creator", Map.of("id", testCreator.getId().toString()));
        return payload;
    }

    // =============== LOCATION VALIDATION TESTS ===============

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldAcceptValidCoordinates() throws Exception {
        Map<String, Object> validLocation = new HashMap<>();
        validLocation.put("latitude", 40.416775);
        validLocation.put("longitude", -3.703790);

        Map<String, Object> questionPayload = createValidQuestionPayload();
        questionPayload.put("location", validLocation);

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.location.latitude").value(40.416775))
                .andExpect(jsonPath("$.location.longitude").value(-3.703790));
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldRejectInvalidLatitudeTooHigh() throws Exception {
        Map<String, Object> invalidLocation = new HashMap<>();
        invalidLocation.put("latitude", 91.0); // Invalid: exceeds max
        invalidLocation.put("longitude", 0.0);

        Map<String, Object> questionPayload = createValidQuestionPayload();
        questionPayload.put("location", invalidLocation);

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldRejectInvalidLatitudeTooLow() throws Exception {
        Map<String, Object> invalidLocation = new HashMap<>();
        invalidLocation.put("latitude", -91.0); // Invalid: below min
        invalidLocation.put("longitude", 0.0);

        Map<String, Object> questionPayload = createValidQuestionPayload();
        questionPayload.put("location", invalidLocation);

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldRejectInvalidLongitudeTooHigh() throws Exception {
        Map<String, Object> invalidLocation = new HashMap<>();
        invalidLocation.put("latitude", 0.0);
        invalidLocation.put("longitude", 181.0); // Invalid: exceeds max

        Map<String, Object> questionPayload = createValidQuestionPayload();
        questionPayload.put("location", invalidLocation);

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldRejectInvalidLongitudeTooLow() throws Exception {
        Map<String, Object> invalidLocation = new HashMap<>();
        invalidLocation.put("latitude", 0.0);
        invalidLocation.put("longitude", -181.0); // Invalid: below min

        Map<String, Object> questionPayload = createValidQuestionPayload();
        questionPayload.put("location", invalidLocation);

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldRejectLocationWithNullLatitude() throws Exception {
        Map<String, Object> invalidLocation = new HashMap<>();
        invalidLocation.put("latitude", null);
        invalidLocation.put("longitude", 0.0);

        Map<String, Object> questionPayload = createValidQuestionPayload();
        questionPayload.put("location", invalidLocation);

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldRejectLocationWithNullLongitude() throws Exception {
        Map<String, Object> invalidLocation = new HashMap<>();
        invalidLocation.put("latitude", 0.0);
        invalidLocation.put("longitude", null);

        Map<String, Object> questionPayload = createValidQuestionPayload();
        questionPayload.put("location", invalidLocation);

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldAcceptBoundaryValues() throws Exception {
        Map<String, Object> boundaryLocation = new HashMap<>();
        boundaryLocation.put("latitude", 90.0); // Max valid
        boundaryLocation.put("longitude", 180.0); // Max valid

        Map<String, Object> questionPayload = createValidQuestionPayload();
        questionPayload.put("location", boundaryLocation);

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.location.latitude").value(90.0))
                .andExpect(jsonPath("$.location.longitude").value(180.0));
    }

    @Test
    @WithMockUser(username = "testcreator@streetask.com")
    void create_shouldAcceptNegativeBoundaryValues() throws Exception {
        Map<String, Object> boundaryLocation = new HashMap<>();
        boundaryLocation.put("latitude", -90.0); // Min valid
        boundaryLocation.put("longitude", -180.0); // Min valid

        Map<String, Object> questionPayload = createValidQuestionPayload();
        questionPayload.put("location", boundaryLocation);

        mockMvc.perform(post("/api/v1/questions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.location.latitude").value(-90.0))
                .andExpect(jsonPath("$.location.longitude").value(-180.0));
    }

}
