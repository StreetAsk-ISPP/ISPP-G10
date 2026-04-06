package com.streetask.app.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.functionalities.email.EmailService;
import com.streetask.app.functionalities.notifications.model.Notification;
import com.streetask.app.functionalities.notifications.model.NotificationRepository;
import com.streetask.app.functionalities.notifications.model.NotificationType;
import com.streetask.app.functionalities.notifications.realtime.FrontendNotificationGateway;
import com.streetask.app.functionalities.notifications.realtime.FrontendNotificationMessage;
import com.streetask.app.model.Strike;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;
import com.streetask.app.user.UserService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModerationService Unit Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class ModerationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StrikeRepository strikeRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private FrontendNotificationGateway frontendNotificationGateway;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ModerationService moderationService;

    private User adminUser;
    private RegularUser regularUser;
    private UUID adminId;
    private UUID regularUserId;

    private static final String ADMIN_EMAIL = "admin@test.com";
    private static final String REGULAR_USER_EMAIL = "user@test.com";

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        regularUserId = UUID.randomUUID();

        // Create admin user
        adminUser = new User();
        adminUser.setId(adminId);
        adminUser.setEmail(ADMIN_EMAIL);
        adminUser.setUserName("admin");
        Authorities adminAuthority = new Authorities();
        adminAuthority.setAuthority("ADMIN");
        adminUser.setAuthority(adminAuthority);

        // Create regular user
        regularUser = new RegularUser();
        regularUser.setId(regularUserId);
        regularUser.setEmail(REGULAR_USER_EMAIL);
        regularUser.setUserName("regularuser");
        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");
        regularUser.setAuthority(userAuthority);

        // Setup security context with admin
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(ADMIN_EMAIL);
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ================= ISSUE STRIKE TESTS =================

    @Test
    @DisplayName("issueStrike should create strike successfully when admin issues strike to regular user")
    void issueStrike_shouldCreateStrikeSuccessfully() {
        // Arrange
        String reason = "Inappropriate content";
        String description = "Violated community guidelines";

        when(userService.findCurrentUser()).thenReturn(adminUser);
        when(userService.findUser(regularUserId)).thenReturn(regularUser);

        Strike expectedStrike = new Strike();
        expectedStrike.setId(UUID.randomUUID());
        expectedStrike.setUser(regularUser);
        expectedStrike.setIssuedBy(adminUser);
        expectedStrike.setReason(reason);
        expectedStrike.setDescription(description);

        when(strikeRepository.save(any(Strike.class))).thenReturn(expectedStrike);
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());
        doNothing().when(frontendNotificationGateway).sendToUser(anyString(), any(FrontendNotificationMessage.class));

        // Act
        Strike result = moderationService.issueStrike(regularUserId, reason, description);

        // Assert
        assertNotNull(result);
        assertEquals(regularUser, result.getUser());
        assertEquals(adminUser, result.getIssuedBy());
        assertEquals(reason, result.getReason());
        assertEquals(description, result.getDescription());

        verify(strikeRepository).save(any(Strike.class));
        verify(notificationRepository).save(any(Notification.class));
        verify(frontendNotificationGateway).sendToUser(eq(REGULAR_USER_EMAIL), any(FrontendNotificationMessage.class));
    }

    @Test
    @DisplayName("issueStrike should throw exception when non-admin tries to issue strike")
    void issueStrike_shouldThrowExceptionWhenNonAdminIssuesStrike() {
        // Arrange
        User nonAdminUser = new User();
        nonAdminUser.setId(UUID.randomUUID());
        nonAdminUser.setEmail("user@test.com");
        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");
        nonAdminUser.setAuthority(userAuthority);

        when(userService.findCurrentUser()).thenReturn(nonAdminUser);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
            moderationService.issueStrike(regularUserId, "Spam", "Too many messages")
        );

        verify(strikeRepository, never()).save(any());
    }

    @Test
    @DisplayName("issueStrike should throw exception when trying to strike non-regular user")
    void issueStrike_shouldThrowExceptionWhenStrikingNonRegularUser() {
        // Arrange
        User businessUser = new User();
        businessUser.setId(UUID.randomUUID());
        businessUser.setEmail("business@test.com");

        when(userService.findCurrentUser()).thenReturn(adminUser);
        when(userService.findUser(businessUser.getId())).thenReturn(businessUser);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
            moderationService.issueStrike(businessUser.getId(), "Spam", "")
        );

        verify(strikeRepository, never()).save(any());
    }

    // ================= DELETE USER TESTS =================

    @Test
    @DisplayName("deleteRegularUser should delete user and associated data successfully")
    void deleteRegularUser_shouldDeleteSuccessfully() {
        // Arrange
        when(userService.findCurrentUser()).thenReturn(adminUser);
        when(userService.findUser(regularUserId)).thenReturn(regularUser);

        List<Strike> userStrikes = Arrays.asList(
            createStrike(adminUser, regularUser, "Spam"),
            createStrike(adminUser, regularUser, "Abuse")
        );

        when(strikeRepository.findByUserOrderByIssuedAtDesc(regularUser))
            .thenReturn(userStrikes);
        doNothing().when(strikeRepository).deleteAll(userStrikes);
        doNothing().when(notificationRepository).deleteByUser(regularUser);
        doNothing().when(userRepository).delete(regularUser);
        doNothing().when(emailService).sendAccountDeletionEmail(REGULAR_USER_EMAIL);

        // Act
        moderationService.deleteRegularUser(regularUserId);

        // Assert
        verify(strikeRepository).findByUserOrderByIssuedAtDesc(regularUser);
        verify(strikeRepository).deleteAll(userStrikes);
        verify(notificationRepository).deleteByUser(regularUser);
        verify(userRepository).delete(regularUser);
        verify(emailService).sendAccountDeletionEmail(REGULAR_USER_EMAIL);
    }

    @Test
    @DisplayName("deleteRegularUser should throw exception when non-admin tries to delete")
    void deleteRegularUser_shouldThrowExceptionWhenNonAdminDeletes() {
        // Arrange
        User nonAdminUser = new User();
        nonAdminUser.setId(UUID.randomUUID());
        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");
        nonAdminUser.setAuthority(userAuthority);

        when(userService.findCurrentUser()).thenReturn(nonAdminUser);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
            moderationService.deleteRegularUser(regularUserId)
        );

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteRegularUser should throw exception when admin tries to delete themselves")
    void deleteRegularUser_shouldThrowExceptionWhenAdminDeletesThemselves() {
        // Arrange
        when(userService.findCurrentUser()).thenReturn(adminUser);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
            moderationService.deleteRegularUser(adminId)
        );

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteRegularUser should throw exception when trying to delete non-regular user")
    void deleteRegularUser_shouldThrowExceptionWhenDeletingNonRegularUser() {
        // Arrange
        User businessUser = new User();
        businessUser.setId(UUID.randomUUID());

        when(userService.findCurrentUser()).thenReturn(adminUser);
        when(userService.findUser(businessUser.getId())).thenReturn(businessUser);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
            moderationService.deleteRegularUser(businessUser.getId())
        );

        verify(userRepository, never()).delete(any());
    }

    // ================= GET STRIKE COUNT TESTS =================

    @Test
    @DisplayName("getStrikeCount should return correct strike count")
    void getStrikeCount_shouldReturnCorrectCount() {
        // Arrange
        when(userService.findCurrentUser()).thenReturn(adminUser);
        when(userService.findUser(regularUserId)).thenReturn(regularUser);
        when(strikeRepository.countByUser(regularUser)).thenReturn(3L);

        // Act
        long count = moderationService.getStrikeCount(regularUserId);

        // Assert
        assertEquals(3L, count);
        verify(strikeRepository).countByUser(regularUser);
    }

    @Test
    @DisplayName("getStrikeCount should throw exception for non-admin")
    void getStrikeCount_shouldThrowExceptionForNonAdmin() {
        // Arrange
        User nonAdminUser = new User();
        nonAdminUser.setId(UUID.randomUUID());
        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");
        nonAdminUser.setAuthority(userAuthority);

        when(userService.findCurrentUser()).thenReturn(nonAdminUser);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
            moderationService.getStrikeCount(regularUserId)
        );
    }

    // ================= GET USER STRIKES TESTS =================

    @Test
    @DisplayName("getUserStrikes should return all strikes for user")
    void getUserStrikes_shouldReturnAllStrikes() {
        // Arrange
        when(userService.findCurrentUser()).thenReturn(adminUser);
        when(userService.findUser(regularUserId)).thenReturn(regularUser);

        List<Strike> strikes = Arrays.asList(
            createStrike(adminUser, regularUser, "Spam"),
            createStrike(adminUser, regularUser, "Abuse")
        );

        when(strikeRepository.findByUserOrderByIssuedAtDesc(regularUser))
            .thenReturn(strikes);

        // Act
        List<Strike> result = moderationService.getUserStrikes(regularUserId);

        // Assert
        assertEquals(2, result.size());
        verify(strikeRepository).findByUserOrderByIssuedAtDesc(regularUser);
    }

    @Test
    @DisplayName("getUserStrikes should return empty list when user has no strikes")
    void getUserStrikes_shouldReturnEmptyListWhenNoStrikes() {
        // Arrange
        when(userService.findCurrentUser()).thenReturn(adminUser);
        when(userService.findUser(regularUserId)).thenReturn(regularUser);
        when(strikeRepository.findByUserOrderByIssuedAtDesc(regularUser))
            .thenReturn(Collections.emptyList());

        // Act
        List<Strike> result = moderationService.getUserStrikes(regularUserId);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("getUserStrikes should throw exception for non-admin")
    void getUserStrikes_shouldThrowExceptionForNonAdmin() {
        // Arrange
        User nonAdminUser = new User();
        nonAdminUser.setId(UUID.randomUUID());
        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");
        nonAdminUser.setAuthority(userAuthority);

        when(userService.findCurrentUser()).thenReturn(nonAdminUser);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
            moderationService.getUserStrikes(regularUserId)
        );
    }

    // ================= HELPER METHODS =================

    private Strike createStrike(User issuedBy, RegularUser user, String reason) {
        Strike strike = new Strike();
        strike.setId(UUID.randomUUID());
        strike.setIssuedBy(issuedBy);
        strike.setUser(user);
        strike.setReason(reason);
        strike.setIssuedAt(LocalDateTime.now());
        return strike;
    }
}
