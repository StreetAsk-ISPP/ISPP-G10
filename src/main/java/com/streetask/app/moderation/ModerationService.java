package com.streetask.app.moderation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.functionalities.email.EmailService;
import com.streetask.app.functionalities.notifications.model.Notification;
import com.streetask.app.functionalities.notifications.model.NotificationRepository;
import com.streetask.app.functionalities.notifications.model.NotificationType;
import com.streetask.app.functionalities.notifications.realtime.FrontendNotificationGateway;
import com.streetask.app.functionalities.notifications.realtime.FrontendNotificationMessage;
import com.streetask.app.model.Strike;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;
import com.streetask.app.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private static final Logger logger = LoggerFactory.getLogger(ModerationService.class);
    private final UserService userService;
    private final UserRepository userRepository;
    private final StrikeRepository strikeRepository;
    private final NotificationRepository notificationRepository;
    private final FrontendNotificationGateway frontendNotificationGateway;
    private final EmailService emailService;

    @Transactional
    public Strike issueStrike(UUID userId, String reason, String description) {
        User currentUser = userService.findCurrentUser();
        if (!currentUser.hasAuthority("ADMIN")) {
            logger.warn("[ModerationService] Non-admin user {} tried to issue strike", currentUser.getId());
            throw new AccessDeniedException("Only admins can send strikes");
        }

        RegularUser targetUser = getModeratableUser(userId);

        Strike strike = new Strike();
        strike.setUser(targetUser);
        strike.setIssuedBy(currentUser);
        strike.setReason(reason);
        strike.setDescription(description);
        strike.setIssuedAt(LocalDateTime.now());
        Strike savedStrike = strikeRepository.save(strike);

        Notification notification = new Notification();
        notification.setUser(targetUser);
        notification.setType(NotificationType.STRIKE);
        notification.setContent("You have received a moderation strike: " + reason);
        notification.setReferenceId(savedStrike.getId());
        notification.setReferenceType("STRIKE");
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);

        FrontendNotificationMessage websocketMessage = FrontendNotificationMessage.builder()
                .type("STRIKE")
                .title("Moderation warning")
                .message("You have received a moderation strike: " + reason)
                .referenceId(savedStrike.getId())
                .referenceType("STRIKE")
                .timestamp(LocalDateTime.now())
                .build();
        frontendNotificationGateway.sendToUser(targetUser.getEmail(), websocketMessage);

        return savedStrike;
    }

    @Transactional
    public void deleteRegularUser(UUID userId) {
        User currentUser = userService.findCurrentUser();

        if (!currentUser.hasAuthority("ADMIN")) {
            logger.warn("[ModerationService] Non-admin user {} tried to delete user {}", currentUser.getId(), userId);
            throw new AccessDeniedException("Only admins can delete users");
        }

        if (currentUser.getId().equals(userId)) {
            logger.warn("[ModerationService] Admin {} attempted to delete themselves", currentUser.getId());
            throw new AccessDeniedException("Admins cannot delete themselves");
        }

        RegularUser targetUser = getModeratableUser(userId);
        String userEmail = targetUser.getEmail();

        List<Strike> userStrikes = strikeRepository.findByUserOrderByIssuedAtDesc(targetUser);
        strikeRepository.deleteAll(userStrikes);

        notificationRepository.deleteByUser(targetUser);
        userRepository.delete(targetUser);
        logger.info("[ModerationService] User deleted successfully: {}", userId);

        emailService.sendAccountDeletionEmail(userEmail);
    }

    @Transactional(readOnly = true)
    public long getStrikeCount(UUID userId) {
        User currentUser = userService.findCurrentUser();
        if (currentUser.getAuthority() != null && !currentUser.hasAuthority("ADMIN")) {
            throw new AccessDeniedException("Only admins can view strike counts");
        }
        RegularUser targetUser = getModeratableUser(userId);
        return strikeRepository.countByUser(targetUser);
    }

    @Transactional(readOnly = true)
    public List<Strike> getUserStrikes(UUID userId) {
        User currentUser = userService.findCurrentUser();
        if (!currentUser.hasAuthority("ADMIN")) {
            throw new AccessDeniedException("Only admins can view strikes");
        }
        RegularUser targetUser = getModeratableUser(userId);
        return strikeRepository.findByUserOrderByIssuedAtDesc(targetUser);
    }

    private RegularUser getModeratableUser(UUID userId) {
        User targetUser = userService.findUser(userId);
        if (!(targetUser instanceof RegularUser regularUser)) {
            logger.warn("[ModerationService] Attempted to moderate non-regular user: {}", userId);
            throw new AccessDeniedException("Moderation actions only apply to regular users");
        }
        return regularUser;
    }
}