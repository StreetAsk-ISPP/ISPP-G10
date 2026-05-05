package com.streetask.app.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.streetask.app.answer.AnswerRepository;
import com.streetask.app.business.BusinessAccount;
import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.model.Answer;
import com.streetask.app.model.Question;
import com.streetask.app.payments.StripeRedirectUrlResolver;
import com.streetask.app.question.QuestionRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EntityManager entityManager;

    @Mock
    private StripeRedirectUrlResolver stripeRedirectUrlResolver;

    @Mock
    private Query nativeQuery;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_FIRST_NAME = "Test";
    private static final String TEST_LAST_NAME = "User";

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = createTestUser(testUserId, TEST_EMAIL, TEST_USERNAME);
        SecurityContextHolder.clearContext();

        ReflectionTestUtils.setField(userService, "entityManager", entityManager);
        ReflectionTestUtils.setField(userService, "stripeRedirectUrlResolver", stripeRedirectUrlResolver);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.setParameter(eq("userId"), any())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.executeUpdate()).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ================= GET PASSWORD ENCODER =================

    @Test
    @DisplayName("getPasswordEncoder should return new BCryptPasswordEncoder when field is null")
    void getPasswordEncoder_shouldReturnDefaultWhenNull() {
        PasswordEncoder originalEncoder = (PasswordEncoder) ReflectionTestUtils.getField(userService,
                "passwordEncoder");
        try {
            ReflectionTestUtils.setField(userService, "passwordEncoder", null);

            PasswordEncoder result = ReflectionTestUtils.invokeMethod(userService, "getPasswordEncoder");

            assertNotNull(result);
            assertTrue(result instanceof org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder);
        } finally {
            ReflectionTestUtils.setField(userService, "passwordEncoder", originalEncoder);
        }
    }

    @Test
    @DisplayName("getPasswordEncoder should return the injected encoder when field is NOT null")
    void getPasswordEncoder_shouldReturnInjectedEncoder() {
        PasswordEncoder result = ReflectionTestUtils.invokeMethod(userService, "getPasswordEncoder");

        assertNotNull(result);
        assertEquals(passwordEncoder, result);
    }

    // ================= SAVE USER =================

    @Test
    @DisplayName("saveUser should persist user successfully")
    void saveUser_shouldPersistSuccessfully() {
        User userToSave = createTestUser(UUID.randomUUID(), "newuser@example.com", "newuser");
        when(userRepository.save(any(User.class))).thenReturn(userToSave);

        User savedUser = userService.saveUser(userToSave);

        assertNotNull(savedUser);
        assertEquals(userToSave.getEmail(), savedUser.getEmail());
        assertEquals(userToSave.getUserName(), savedUser.getUserName());
        verify(userRepository).save(userToSave);
    }

    @Test
    @DisplayName("saveUser should throw DataAccessException on database failure")
    void saveUser_shouldThrowDataAccessExceptionOnDatabaseFailure() {
        User userToSave = createTestUser(UUID.randomUUID(), "newuser@example.com", "newuser");

        when(userRepository.save(any(User.class)))
                .thenThrow(new DataAccessException("DB Error") {
                });

        assertThrows(DataAccessException.class, () -> userService.saveUser(userToSave));
        verify(userRepository).save(userToSave);
    }

    // ================= FIND USER =================

    @Test
    @DisplayName("findUser by email should return user when found")
    void findUserByEmail_shouldReturnUserWhenFound() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        User foundUser = userService.findUser(TEST_EMAIL);

        assertNotNull(foundUser);
        assertEquals(TEST_EMAIL, foundUser.getEmail());
        assertEquals(testUserId, foundUser.getId());
        verify(userRepository).findByEmail(TEST_EMAIL);
    }

    @Test
    @DisplayName("findUser by email should throw ResourceNotFoundException when not found")
    void findUserByEmail_shouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.findUser("notfound@example.com"));
    }

    @Test
    @DisplayName("findUser by id should return user when found")
    void findUserById_shouldReturnUserWhenFound() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        User foundUser = userService.findUser(testUserId);

        assertNotNull(foundUser);
        assertEquals(testUserId, foundUser.getId());
    }

    @Test
    @DisplayName("findUser by id should throw ResourceNotFoundException when not found")
    void findUserById_shouldThrowResourceNotFoundExceptionWhenNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.findUser(nonExistentId));
    }

    // ================= CURRENT USER =================

    @Test
    @DisplayName("findCurrentUser should work with UUID identifier")
    void findCurrentUser_shouldWorkWithUuid() {
        String uuidStr = testUserId.toString();
        setupSecurityContext(uuidStr);

        when(userRepository.findByEmailIgnoreCase(uuidStr)).thenReturn(Optional.empty());
        when(userRepository.findByUserNameIgnoreCase(uuidStr)).thenReturn(Optional.empty());
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        User result = userService.findCurrentUser();

        assertEquals(testUserId, result.getId());
    }

    @Test
    @DisplayName("findCurrentUser should return authenticated user")
    void findCurrentUser_shouldReturnAuthenticatedUserWhenPresent() {

        setupSecurityContext(TEST_EMAIL);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        User currentUser = userService.findCurrentUser();

        assertNotNull(currentUser);
        assertEquals(TEST_EMAIL, currentUser.getEmail());
    }

    @Test
    @DisplayName("findCurrentUser should throw ResourceNotFoundException when nobody is authenticated")
    void findCurrentUser_shouldThrowResourceNotFoundExceptionWhenAuthenticationIsMissing() {
        SecurityContextHolder.clearContext();

        assertThrows(ResourceNotFoundException.class, () -> userService.findCurrentUser());
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    @DisplayName("findCurrentUser should throw ResourceNotFoundException when authenticated user is missing in repository")
    void findCurrentUser_shouldThrowResourceNotFoundExceptionWhenRepositoryUserIsMissing() {
        setupSecurityContext(TEST_EMAIL);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findByUserNameIgnoreCase(TEST_EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findCurrentUser());
        verify(userRepository).findByEmailIgnoreCase(TEST_EMAIL);
    }

    // ================= EXISTS =================

    @Test
    void existsUser_shouldReturnTrueWhenUserExistsByEmail() {
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        assertTrue(userService.existsUser(TEST_EMAIL));
    }

    @Test
    void existsByUserName_shouldReturnTrueWhenUserExistsByUsername() {
        when(userRepository.existsByUserName(TEST_USERNAME)).thenReturn(true);

        assertTrue(userService.existsByUserName(TEST_USERNAME));
    }

    // ================= FIND ALL =================

    @Test
    void findAll_shouldReturnAllUsers() {

        List<User> users = new ArrayList<>();
        users.add(testUser);
        users.add(createTestUser(UUID.randomUUID(), "user2@example.com", "user2"));

        when(userRepository.findAll()).thenReturn(users);

        Iterable<User> result = userService.findAll();

        assertNotNull(result);
        verify(userRepository).findAll();
    }

    @Test
    void findAllByAuthority_shouldReturnFilteredUsersWithReputation() {
        UUID regularUserId = UUID.randomUUID();
        User regularUser = createTestUserWithAuthority(regularUserId, "regular@example.com", "regular", "USER");

        when(userRepository.findAllByAuthority("USER")).thenReturn(Collections.singletonList(regularUser));
        when(answerRepository.aggregateVotesByUserIds(eq(List.of(regularUserId))))
                .thenReturn(Collections.singletonList(new Object[] { regularUserId, 4L, 1L }));

        List<User> result = (List<User>) userService.findAllByAuthority("USER");

        assertEquals(1, result.size());
        assertEquals(7, result.get(0).getReputation());
        verify(userRepository).findAllByAuthority("USER");
        verify(answerRepository).aggregateVotesByUserIds(eq(List.of(regularUserId)));
    }

    // ================= UPDATE =================

    @Test
    void updateUser_shouldPreserveOriginalIdWhenUpdatingUser() {
        UserUpdateRequest update = new UserUpdateRequest();
        update.setEmail("new@mail.com");
        update.setUserName("newuser");
        update.setFirstName(TEST_FIRST_NAME);
        update.setLastName(TEST_LAST_NAME);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        User updated = userService.updateUser(update, testUserId);

        assertEquals(testUserId, updated.getId());
    }

    @Test
    void updateUser_shouldUpdateEditableFieldsAndKeepOldPassword() {
        User originalUser = createTestUser(UUID.randomUUID(), "old@example.com", "olduser");
        originalUser.setPassword("old_encoded_password");

        UserUpdateRequest incomingUpdate = new UserUpdateRequest();
        incomingUpdate.setFirstName("NewFirst");
        incomingUpdate.setLastName("NewLast");
        incomingUpdate.setUserName("newuser");
        incomingUpdate.setEmail("new@example.com");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(originalUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User updatedUser = userService.updateUser(incomingUpdate, testUserId);

        assertEquals("NewFirst", updatedUser.getFirstName());
        assertEquals("NewLast", updatedUser.getLastName());
        assertEquals("newuser", updatedUser.getUserName());
        assertEquals("new@example.com", updatedUser.getEmail());

        assertEquals("old_encoded_password", updatedUser.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("updateUser should not modify password when updating profile fields")
    void updateUser_shouldNotModifyPassword() {
        User originalUser = createTestUser(UUID.randomUUID(), "old@example.com", "olduser");
        originalUser.setPassword("existing_encoded_password");

        UserUpdateRequest incomingUpdate = new UserUpdateRequest();
        incomingUpdate.setFirstName("NewFirst");
        incomingUpdate.setLastName("NewLast");
        incomingUpdate.setUserName("newuser");
        incomingUpdate.setEmail("new@example.com");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(originalUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User updatedUser = userService.updateUser(incomingUpdate, testUserId);

        assertEquals("existing_encoded_password", updatedUser.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("updateUser should preserve existing password regardless of what is in the request")
    void updateUser_shouldAlwaysPreserveExistingPassword() {
        User originalUser = createTestUser(UUID.randomUUID(), "old@example.com", "olduser");
        originalUser.setPassword("$2a$10$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu");

        UserUpdateRequest incomingUpdate = new UserUpdateRequest();
        incomingUpdate.setFirstName("NewFirst");
        incomingUpdate.setLastName("NewLast");
        incomingUpdate.setUserName("newuser");
        incomingUpdate.setEmail("new@example.com");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(originalUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User updatedUser = userService.updateUser(incomingUpdate, testUserId);

        assertEquals("$2a$10$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu", updatedUser.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("updateUser should NOT modify protected fields (authorities, active, createdAt, id)")
    void updateUser_shouldNotModifyProtectedFields() {
        LocalDateTime oldDate = LocalDateTime.of(2020, 1, 1, 0, 0);
        Authorities oldAuth = new Authorities();
        oldAuth.setAuthority("USER");

        User originalUser = createTestUser(testUserId, "old@example.com", "olduser");
        originalUser.setCreatedAt(oldDate);
        originalUser.setAuthority(oldAuth);
        originalUser.setActive(true);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setFirstName("Hacker");
        request.setLastName("Man");
        request.setUserName("hacker");
        request.setEmail("hacker@mail.com");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(originalUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User updatedUser = userService.updateUser(request, testUserId);

        assertEquals("Hacker", updatedUser.getFirstName(), "Editable fields should update");

        assertEquals(testUserId, updatedUser.getId(), "ID should not be modified");
        assertEquals("USER", updatedUser.getAuthority().getAuthority(), "Authority should not be modified");
        assertTrue(updatedUser.getActive(), "Active status should not be modified");
        assertEquals(oldDate, updatedUser.getCreatedAt(), "Creation date should not be modified");
    }

    // ================= DELETE =================

    @Test
    void deleteUser_shouldSuccessfullyDeleteUserWhenFound() {

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        userService.deleteUser(testUserId);

        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_shouldCleanupBusinessAccountData() {
        BusinessAccount business = new BusinessAccount();
        business.setId(testUserId);
        business.setAccountType(AccountType.BUSINESS);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(business));

        userService.deleteUser(testUserId);

        verify(userRepository).delete(business);
    }

    @Test
    void deleteUser_shouldCleanupAdminData() {
        Admin admin = new Admin();
        admin.setId(testUserId);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(admin));

        userService.deleteUser(testUserId);

        verify(userRepository).delete(admin);
    }

    @Test
    void deleteCurrentUserAccount_shouldWork() {
        setupSecurityContext(TEST_EMAIL);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        userService.deleteCurrentUserAccount();

        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("deleteUser should cleanup all RegularUser data including questions and answers")
    void deleteUserAccount_shouldCleanupRegularUserData() {
        RegularUser regularUser = new RegularUser();
        regularUser.setId(testUserId);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(regularUser));

        userService.deleteUser(testUserId);

        verify(userRepository).delete(regularUser);
    }

    // ================= REPUTATION TESTS (TRUNK) =================

    @Test
    void findUserById_shouldIncludeReputation() {

        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(answerRepository.aggregateVotesByUserIds(anyCollection())).thenReturn(Collections.singletonList(
                new Object[] { userId, 6L, 0L }));

        User result = userService.findUser(userId);

        assertEquals(12, result.getReputation());
        verify(answerRepository).aggregateVotesByUserIds(anyCollection());
    }

    @Test
    @DisplayName("findAll should return empty list when no users exist")
    void findAll_shouldReturnEmptyListWhenNoUsers() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        Iterable<User> result = userService.findAll();

        assertNotNull(result);
        List<User> resultList = (List<User>) result;
        assertTrue(resultList.isEmpty());

        verify(answerRepository, never()).aggregateVotesByUserIds(any());
    }

    @Test
    void findAll_shouldIncludeReputationForEveryUser() {

        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        User first = new User();
        first.setId(firstId);

        User second = new User();
        second.setId(secondId);

        List<User> users = Arrays.asList(first, second);

        when(userRepository.findAll()).thenReturn(users);
        when(answerRepository.aggregateVotesByUserIds(anyCollection())).thenReturn(Arrays.asList(
                new Object[] { firstId, 3L, 1L },
                new Object[] { secondId, 0L, 2L }));

        Iterable<User> result = userService.findAll();

        User[] arr = ((List<User>) result).toArray(new User[0]);

        assertEquals(5, arr[0].getReputation());
        assertEquals(-2, arr[1].getReputation());
    }

    @Test
    void findUserById_shouldDefaultReputationToZeroWhenVotesAreMissing() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(answerRepository.aggregateVotesByUserIds(eq(List.of(userId)))).thenReturn(Collections.emptyList());

        User result = userService.findUser(userId);

        assertEquals(0, result.getReputation());
        verify(answerRepository).aggregateVotesByUserIds(eq(List.of(userId)));
    }

    @Test
    void findAll_shouldDefaultReputationToZeroWhenAUserHasNoAggregates() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        User first = new User();
        first.setId(firstId);

        User second = new User();
        second.setId(secondId);

        List<User> users = Arrays.asList(first, second);

        when(userRepository.findAll()).thenReturn(users);
        when(answerRepository.aggregateVotesByUserIds(anyCollection())).thenReturn(Collections.singletonList(
                new Object[] { firstId, 2L, 0L }));

        Iterable<User> result = userService.findAll();

        User[] resultArray = ((List<User>) result).toArray(new User[0]);
        assertEquals(4, resultArray[0].getReputation());
        assertEquals(0, resultArray[1].getReputation());
        verify(answerRepository).aggregateVotesByUserIds(anyCollection());
    }

    @Test
    void findUserById_shouldApplyFormulaLikesTimesTwoMinusDislikes() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(answerRepository.aggregateVotesByUserIds(eq(List.of(userId))))
                .thenReturn(Collections.singletonList(new Object[] { userId, 9L, 4L }));

        User result = userService.findUser(userId);

        assertEquals(14, result.getReputation());
        verify(answerRepository).aggregateVotesByUserIds(eq(List.of(userId)));
    }

    @Test
    void findAll_shouldHandlePositiveAndNegativeReputationScenarios() {
        UUID positiveUserId = UUID.randomUUID();
        UUID negativeUserId = UUID.randomUUID();

        User positiveUser = new User();
        positiveUser.setId(positiveUserId);

        User negativeUser = new User();
        negativeUser.setId(negativeUserId);

        when(userRepository.findAll()).thenReturn(Arrays.asList(positiveUser, negativeUser));
        when(answerRepository.aggregateVotesByUserIds(anyCollection())).thenReturn(Arrays.asList(
                new Object[] { positiveUserId, 7L, 1L },
                new Object[] { negativeUserId, 1L, 6L }));

        List<User> result = (List<User>) userService.findAll();

        assertEquals(13, result.get(0).getReputation());
        assertEquals(-4, result.get(1).getReputation());
    }

    @Test
    void findUserById_shouldRecalculateReputationConsistentlyWhenAggregatesChange() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(answerRepository.aggregateVotesByUserIds(eq(List.of(userId))))
                .thenReturn(Collections.singletonList(new Object[] { userId, 3L, 1L }))
                .thenReturn(Collections.singletonList(new Object[] { userId, 4L, 2L }));

        User firstRead = userService.findUser(userId);
        int firstReputation = firstRead.getReputation();
        User secondRead = userService.findUser(userId);

        assertEquals(5, firstReputation);
        assertEquals(6, secondRead.getReputation());
        verify(answerRepository, times(2)).aggregateVotesByUserIds(eq(List.of(userId)));
    }

    // ================= USER STATS =================

    @Test
    @DisplayName("getUserStats should return correct stats for a user with activity")
    void getUserStats_shouldReturnCorrectStatsForUserWithActivity() {
        RegularUser user = createTestRegularUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "USER");
        user.setTotalLikesReceived(8);
        user.setTotalDislikesReceived(2);
        user.setCoinBalance(13);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));
        when(questionRepository.countByCreatorId(testUserId)).thenReturn(5L);
        when(answerRepository.countByUserId(testUserId)).thenReturn(10L);

        Map<String, Object> stats = userService.getUserStats(testUserId);

        assertNotNull(stats);
        assertEquals(5L, stats.get("questionsCount"));
        assertEquals(10L, stats.get("answersCount"));
        assertEquals(TEST_USERNAME, stats.get("username"));
        assertEquals("USER", stats.get("role"));
        assertEquals(8, stats.get("likesCount"));
        assertEquals(2, stats.get("dislikesCount"));
        assertEquals(13, stats.get("coinBalance"));
        assertNotNull(stats.get("reputation"));
        assertEquals(4.0, stats.get("rating"));
        verify(questionRepository).countByCreatorId(testUserId);
        verify(answerRepository).countByUserId(testUserId);
    }

    @Test
    @DisplayName("getUserStats should return zero counts for user with no activity")
    void getUserStats_shouldReturnZeroCountsForUserWithNoActivity() {
        RegularUser user = createTestRegularUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "USER");
        user.setTotalLikesReceived(0);
        user.setTotalDislikesReceived(0);
        user.setCoinBalance(0);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));
        when(questionRepository.countByCreatorId(testUserId)).thenReturn(0L);
        when(answerRepository.countByUserId(testUserId)).thenReturn(0L);

        Map<String, Object> stats = userService.getUserStats(testUserId);

        assertNotNull(stats);
        assertEquals(0L, stats.get("questionsCount"));
        assertEquals(0L, stats.get("answersCount"));
        assertEquals(0, stats.get("likesCount"));
        assertEquals(0, stats.get("dislikesCount"));
        assertEquals(0, stats.get("coinBalance"));
        assertEquals(0.0, stats.get("rating"));
    }

    @Test
    @DisplayName("getUserStats should cap rating at 5.0 - Forced branch")
    void getUserStats_shouldCapRatingAtFive() {
        RegularUser user = new RegularUser() {
            @Override
            public Integer getTotalLikesReceived() {
                return 10;
            }

            @Override
            public Integer getTotalDislikesReceived() {
                return -1;
            }
        };
        user.setId(testUserId);
        user.setUserName(TEST_USERNAME);
        user.setAuthority(new Authorities());

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));

        Map<String, Object> stats = userService.getUserStats(testUserId);

        assertEquals(5.0, stats.get("rating"));
    }

    @Test
    @DisplayName("getUserStats should return correct role for ADMIN user")
    void getUserStats_shouldReturnAdminRole() {
        User user = createTestUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "ADMIN");
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));
        when(questionRepository.countByCreatorId(testUserId)).thenReturn(0L);
        when(answerRepository.countByUserId(testUserId)).thenReturn(0L);

        Map<String, Object> stats = userService.getUserStats(testUserId);

        assertEquals("ADMIN", stats.get("role"));
    }

    @Test
    @DisplayName("getUserStats should throw ResourceNotFoundException for non-existent user")
    void getUserStats_shouldThrowResourceNotFoundExceptionForNonExistentUser() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserStats(nonExistentId));
    }

    // ================= FIND QUESTIONS BY USER =================

    @Test
    @DisplayName("findQuestionsByUserId should return questions for user")
    void findQuestionsByUserId_shouldReturnQuestionsForUser() {
        Question q1 = new Question();
        q1.setId(UUID.randomUUID());
        q1.setTitle("Question 1");

        Question q2 = new Question();
        q2.setId(UUID.randomUUID());
        q2.setTitle("Question 2");

        List<Question> questions = Arrays.asList(q1, q2);
        when(questionRepository.findByCreatorId(testUserId)).thenReturn(questions);

        Iterable<Question> result = userService.findQuestionsByUserId(testUserId);

        assertNotNull(result);
        List<Question> resultList = new ArrayList<>();
        result.forEach(resultList::add);
        assertEquals(2, resultList.size());
        verify(questionRepository).findByCreatorId(testUserId);
    }

    @Test
    @DisplayName("findQuestionsByUserId should return empty list when user has no questions")
    void findQuestionsByUserId_shouldReturnEmptyListWhenNoQuestions() {
        when(questionRepository.findByCreatorId(testUserId)).thenReturn(Collections.emptyList());

        Iterable<Question> result = userService.findQuestionsByUserId(testUserId);

        assertNotNull(result);
        List<Question> resultList = new ArrayList<>();
        result.forEach(resultList::add);
        assertTrue(resultList.isEmpty());
    }

    // ================= FIND ANSWERS BY USER =================

    @Test
    @DisplayName("findAnswersByUserId should return answers for user")
    void findAnswersByUserId_shouldReturnAnswersForUser() {
        Answer a1 = new Answer();
        a1.setId(UUID.randomUUID());
        a1.setContent("Answer 1");

        Answer a2 = new Answer();
        a2.setId(UUID.randomUUID());
        a2.setContent("Answer 2");

        List<Answer> answers = Arrays.asList(a1, a2);
        when(answerRepository.findByUserId(testUserId)).thenReturn(answers);

        Iterable<com.streetask.app.model.Answer> result = userService.findAnswersByUserId(testUserId);

        assertNotNull(result);
        List<com.streetask.app.model.Answer> resultList = new ArrayList<>();
        result.forEach(resultList::add);
        assertEquals(2, resultList.size());
        verify(answerRepository).findByUserId(testUserId);
    }

    @Test
    @DisplayName("findAnswersByUserId should return empty list when user has no answers")
    void findAnswersByUserId_shouldReturnEmptyListWhenNoAnswers() {
        when(answerRepository.findByUserId(testUserId)).thenReturn(Collections.emptyList());

        Iterable<com.streetask.app.model.Answer> result = userService.findAnswersByUserId(testUserId);

        assertNotNull(result);
        List<com.streetask.app.model.Answer> resultList = new ArrayList<>();
        result.forEach(resultList::add);
        assertTrue(resultList.isEmpty());
    }

    // ================= REGULAR PREMIUM ACCESS =================

    @Test
    @DisplayName("create Stripe session should throw AccessDeniedException if premium is already active")
    void createCurrentRegularPremiumStripeCheckoutSessionn_shouldThrowIfAlreadyPremium() {
        RegularUser premiumUser = createTestRegularUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "USER");
        premiumUser.setPremiumActive(true);
        premiumUser.setAccountType(AccountType.REGULAR_USER);

        setupSecurityContext(TEST_EMAIL);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(premiumUser));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> userService.createCurrentRegularPremiumStripeCheckoutSession(null));
        assertThat(exception.getMessage()).isEqualTo("Regular premium access is already active.");
    }

    @Test
    @DisplayName("create Stripe session should return session details")
    void createCurrentRegularPremiumStripeCheckoutSession_shouldReturnResponse() {
        RegularUser regularUser = createTestRegularUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "USER");
        regularUser.setPremiumActive(false);
        regularUser.setAccountType(AccountType.REGULAR_USER);

        setupSecurityContext(TEST_EMAIL);
        ReflectionTestUtils.setField(userService, "stripeSecretKey", "sk_test_mock");

        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(regularUser));

        try (var mockedSession = mockStatic(com.stripe.model.checkout.Session.class)) {
            com.stripe.model.checkout.Session mockSession = mock(com.stripe.model.checkout.Session.class);
            when(mockSession.getId()).thenReturn("sess_123");
            when(mockSession.getUrl()).thenReturn("https://stripe.com/pay");
            when(stripeRedirectUrlResolver.resolveCheckoutBaseUrl(any(), any()))
                    .thenReturn("http://localhost:8081");

            mockedSession
                    .when(() -> com.stripe.model.checkout.Session
                            .create(any(com.stripe.param.checkout.SessionCreateParams.class)))
                    .thenReturn(mockSession);

            StripeCheckoutSessionResponse response = userService
                    .createCurrentRegularPremiumStripeCheckoutSession("http://return.url");

            assertNotNull(response);
            assertEquals("sess_123", response.getSessionId());
        }
    }

    @Test
    @DisplayName("create Stripe session should throw IllegalStateException when Stripe API fails")
    void createCurrentRegularPremiumStripeCheckoutSession_shouldHandleStripeException() {
        RegularUser regularUser = createTestRegularUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "USER");
        regularUser.setPremiumActive(false);
        regularUser.setAccountType(AccountType.REGULAR_USER);
        setupSecurityContext(TEST_EMAIL);
        ReflectionTestUtils.setField(userService, "stripeSecretKey", "sk_test_mock");
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(regularUser));

        try (var mockedSession = mockStatic(com.stripe.model.checkout.Session.class)) {
            mockedSession.when(() -> com.stripe.model.checkout.Session
                    .create(any(com.stripe.param.checkout.SessionCreateParams.class)))
                    .thenThrow(mock(com.stripe.exception.StripeException.class));

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> userService.createCurrentRegularPremiumStripeCheckoutSession(null));
            assertThat(exception.getMessage()).isEqualTo("Unable to create Stripe checkout session.");
        }
    }

    @Test
    @DisplayName("createCurrentRegularPremiumStripeCheckoutSession (no args) should call overloaded method with null")
    void createCurrentRegularPremiumStripeCheckoutSession_noArgs_shouldWork() {
        RegularUser regularUser = createTestRegularUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "USER");
        regularUser.setPremiumActive(false);
        regularUser.setAccountType(AccountType.REGULAR_USER);

        setupSecurityContext(TEST_EMAIL);
        ReflectionTestUtils.setField(userService, "stripeSecretKey", "sk_test_mock");
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(regularUser));

        when(stripeRedirectUrlResolver.resolveCheckoutBaseUrl(any(), any()))
                .thenReturn("http://localhost:8081");

        try (var mockedSession = mockStatic(com.stripe.model.checkout.Session.class)) {
            com.stripe.model.checkout.Session mockSess = mock(com.stripe.model.checkout.Session.class);
            when(mockSess.getId()).thenReturn("sess_123");
            mockedSession.when(() -> com.stripe.model.checkout.Session
                    .create(any(com.stripe.param.checkout.SessionCreateParams.class)))
                    .thenReturn(mockSess);

            StripeCheckoutSessionResponse response = userService.createCurrentRegularPremiumStripeCheckoutSession();

            assertNotNull(response);
            assertEquals("sess_123", response.getSessionId());
        }
    }

    @Test
    @DisplayName("confirm Stripe session should throw IllegalArgumentException if sessionId is blank")
    void confirmCurrentRegularPremiumStripeCheckoutSession_shouldThrowIfIdIsBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.confirmCurrentRegularPremiumStripeCheckoutSession(" "));

        assertThat(ex.getMessage()).isEqualTo("Stripe sessionId is required.");
    }

    @Test
    @DisplayName("confirm Stripe session should activate premium")
    void confirmCurrentRegularPremiumStripeCheckoutSession_shouldWork() {
        RegularUser regularUser = createTestRegularUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "USER");
        regularUser.setAccountType(AccountType.REGULAR_USER);

        setupSecurityContext(TEST_EMAIL);
        ReflectionTestUtils.setField(userService, "stripeSecretKey", "sk_test");
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(regularUser));

        com.stripe.model.checkout.Session mockSession = mock(com.stripe.model.checkout.Session.class);
        when(mockSession.getPaymentStatus()).thenReturn("paid");
        when(mockSession.getMetadata()).thenReturn(Map.of("regularUserId", testUserId.toString()));

        try (org.mockito.MockedStatic<com.stripe.model.checkout.Session> mockedSession = mockStatic(
                com.stripe.model.checkout.Session.class)) {
            mockedSession.when(() -> com.stripe.model.checkout.Session.retrieve(anyString())).thenReturn(mockSession);

            userService.confirmCurrentRegularPremiumStripeCheckoutSession("sess_123");

            assertTrue(regularUser.getPremiumActive());
            verify(userRepository).save(regularUser);
        }
    }

    @Test
    @DisplayName("confirm Stripe session should throw AccessDeniedException if payment status is not paid")
    void confirmCurrentRegularPremiumStripeCheckoutSession_shouldThrowIfNotPaid() {
        RegularUser user = createTestRegularUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "USER");
        user.setAccountType(AccountType.REGULAR_USER);
        setupSecurityContext(TEST_EMAIL);
        ReflectionTestUtils.setField(userService, "stripeSecretKey", "sk_test");
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(user));

        try (var mockedSession = mockStatic(com.stripe.model.checkout.Session.class)) {
            com.stripe.model.checkout.Session mockSess = mock(com.stripe.model.checkout.Session.class);
            when(mockSess.getPaymentStatus()).thenReturn("unpaid");

            mockedSession.when(() -> com.stripe.model.checkout.Session.retrieve(anyString()))
                    .thenReturn(mockSess);

            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> userService.confirmCurrentRegularPremiumStripeCheckoutSession("sess_123"));

            assertThat(ex.getMessage()).isEqualTo("Payment has not been completed yet.");
        }
    }

    @Test
    @DisplayName("confirm Stripe session should throw AccessDeniedException if userId mismatch")
    void confirmCurrentRegularPremiumStripeCheckoutSession_shouldThrowIfUserIdMismatch() {
        RegularUser regularUser = createTestRegularUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "USER");
        regularUser.setPremiumActive(false);
        regularUser.setAccountType(AccountType.REGULAR_USER);

        setupSecurityContext(TEST_EMAIL);
        ReflectionTestUtils.setField(userService, "stripeSecretKey", "sk_test_mock");

        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(regularUser));

        try (var mockedSession = mockStatic(com.stripe.model.checkout.Session.class)) {
            com.stripe.model.checkout.Session mockSess = mock(com.stripe.model.checkout.Session.class);

            when(mockSess.getPaymentStatus()).thenReturn("paid");
            when(mockSess.getMetadata()).thenReturn(null);

            mockedSession.when(() -> com.stripe.model.checkout.Session.retrieve(anyString()))
                    .thenReturn(mockSess);

            AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                    () -> userService.confirmCurrentRegularPremiumStripeCheckoutSession("sess_123"));

            assertThat(exception.getMessage()).isEqualTo("Stripe session does not belong to this regular account.");
        }
    }

    @Test
    @DisplayName("confirm Stripe session should throw IllegalStateException on Stripe retrieve error")
    void confirmCurrentRegularPremiumStripeCheckoutSession_shouldHandleStripeException() {
        RegularUser user = createTestRegularUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "USER");
        user.setAccountType(AccountType.REGULAR_USER);
        setupSecurityContext(TEST_EMAIL);
        ReflectionTestUtils.setField(userService, "stripeSecretKey", "sk_test");
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(user));

        try (var mockedSession = mockStatic(com.stripe.model.checkout.Session.class)) {
            mockedSession.when(() -> com.stripe.model.checkout.Session.retrieve(anyString()))
                    .thenThrow(mock(com.stripe.exception.StripeException.class));

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> userService.confirmCurrentRegularPremiumStripeCheckoutSession("sess_123"));
            assertThat(exception.getMessage()).isEqualTo("Unable to confirm Stripe checkout session.");
        }
    }

    @Test
    @DisplayName("updateCurrentRegularPremiumAccess should allow regular users to activate premium")
    void updateCurrentRegularPremiumAccess_shouldAllowRegularUser() {
        RegularUser regularUser = createTestRegularUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "USER");
        regularUser.setAccountType(AccountType.REGULAR_USER);
        regularUser.setPremiumActive(false);

        setupSecurityContext(TEST_EMAIL);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        RegularUser updated = userService.updateCurrentRegularPremiumAccess(true);

        assertTrue(Boolean.TRUE.equals(updated.getPremiumActive()));
        verify(userRepository).save(regularUser);
    }

    @Test
    @DisplayName("updateCurrentRegularPremiumAccess should reject business users")
    void updateCurrentRegularPremiumAccess_shouldRejectBusinessUser() {
        BusinessAccount businessAccount = new BusinessAccount();
        businessAccount.setId(testUserId);
        businessAccount.setEmail(TEST_EMAIL);
        businessAccount.setUserName(TEST_USERNAME);
        businessAccount.setFirstName(TEST_FIRST_NAME);
        businessAccount.setLastName(TEST_LAST_NAME);
        businessAccount.setPassword("password123");
        businessAccount.setAccountType(AccountType.BUSINESS);

        setupSecurityContext(TEST_EMAIL);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(businessAccount));

        assertThrows(AccessDeniedException.class,
                () -> userService.updateCurrentRegularPremiumAccess(true));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Coverage: getCurrentRegularUser should throw if AccountType is wrong")
    void getCurrentRegularUser_WrongAccountType() {
        RegularUser user = createTestRegularUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "USER");
        user.setAccountType(null);

        setupSecurityContext(TEST_EMAIL);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(user));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> userService.createCurrentRegularPremiumStripeCheckoutSession(null));
        assertThat(exception.getMessage()).isEqualTo("Only regular users can access this endpoint.");
    }

    @Test
    @DisplayName("Coverage: ensureStripeConfigured should throw if key is blank")
    void ensureStripeConfigured_shouldThrowIfKeyBlank() {
        RegularUser user = createTestRegularUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "USER");
        user.setAccountType(AccountType.REGULAR_USER);
        setupSecurityContext(TEST_EMAIL);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(user));

        ReflectionTestUtils.setField(userService, "stripeSecretKey", "");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> userService.createCurrentRegularPremiumStripeCheckoutSession(null));
        assertThat(exception.getMessage()).isEqualTo("Stripe secret key is not configured.");
    }

    @Test
    @DisplayName("Coverage: normalizeCurrency and appendQuery defaults and fallbacks")
    void normalizeCurrencyAndAppendQuery_shouldWork() {
        RegularUser user = createTestRegularUserWithAuthority(testUserId, TEST_EMAIL, TEST_USERNAME, "USER");
        user.setAccountType(AccountType.REGULAR_USER);
        setupSecurityContext(TEST_EMAIL);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(user));
        ReflectionTestUtils.setField(userService, "stripeSecretKey", "sk_test");

        try (var mockedSession = mockStatic(com.stripe.model.checkout.Session.class)) {
            com.stripe.model.checkout.Session mockSess = mock(com.stripe.model.checkout.Session.class);
            mockedSession.when(() -> com.stripe.model.checkout.Session
                    .create(any(com.stripe.param.checkout.SessionCreateParams.class)))
                    .thenReturn(mockSess);
            ReflectionTestUtils.setField(userService, "stripeCurrency", "");
            ReflectionTestUtils.setField(userService, "frontendUrl", "");
            when(stripeRedirectUrlResolver.resolveCheckoutBaseUrl(any(), any()))
                    .thenReturn(null, "http://localhost:8081");

            userService.createCurrentRegularPremiumStripeCheckoutSession("http://return.url");

            ReflectionTestUtils.setField(userService, "stripeCurrency", " USD ");
            ReflectionTestUtils.setField(userService, "frontendUrl", "http://mi-frontend.com?param=1 ");
            when(stripeRedirectUrlResolver.resolveCheckoutBaseUrl(any(), any()))
                    .thenReturn(null);

            userService.createCurrentRegularPremiumStripeCheckoutSession("http://return.url");

        }
    }

    @Test
    @DisplayName("resolveRegularPremiumAmount should handle nulls and minimums")
    void resolveRegularPremiumAmount_shouldHandleEdgeCases() {
        ReflectionTestUtils.setField(userService, "stripeRegularPremiumAmountCents", null);
        Long amount = ReflectionTestUtils.invokeMethod(userService, "resolveRegularPremiumAmount");
        assertEquals(999L, amount);

        ReflectionTestUtils.setField(userService, "stripeRegularPremiumAmountCents", -50);
        amount = ReflectionTestUtils.invokeMethod(userService, "resolveRegularPremiumAmount");
        assertEquals(1L, amount);
    }

    // ================= HELPERS =================

    private User createTestUserWithAuthority(UUID id, String email, String userName, String authorityName) {
        User user = createTestUser(id, email, userName);
        Authorities auth = new Authorities();
        auth.setAuthority(authorityName);
        user.setAuthority(auth);
        return user;
    }

    private RegularUser createTestRegularUserWithAuthority(UUID id, String email, String userName,
            String authorityName) {
        RegularUser user = new RegularUser();
        user.setId(id);
        user.setEmail(email);
        user.setUserName(userName);
        user.setFirstName(TEST_FIRST_NAME);
        user.setLastName(TEST_LAST_NAME);
        user.setPassword("password123");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        Authorities auth = new Authorities();
        auth.setAuthority(authorityName);
        user.setAuthority(auth);
        return user;
    }

    private User createTestUser(UUID id, String email, String userName) {

        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUserName(userName);
        user.setFirstName(TEST_FIRST_NAME);
        user.setLastName(TEST_LAST_NAME);
        user.setPassword("password123");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        return user;
    }

    private void setupSecurityContext(String email) {

        SecurityContext context = SecurityContextHolder.createEmptyContext();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);

        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
}
