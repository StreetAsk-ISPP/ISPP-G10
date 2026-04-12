package com.streetask.app.auth;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;

@Component
public class PendingSignupCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(PendingSignupCleanupTask.class);
    private static final int STALE_HOURS = 48;

    private final UserRepository userRepository;

    public PendingSignupCleanupTask(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(fixedRate = 3600000) // every hour
    @Transactional
    public void cleanupStalePendingUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(STALE_HOURS);
        List<User> staleUsers = userRepository.findStalePendingBasicUsers(cutoff);
        if (!staleUsers.isEmpty()) {
            userRepository.deleteAll(staleUsers);
            log.info("Cleaned up {} stale pending basic users older than {}h", staleUsers.size(), STALE_HOURS);
        }
    }
}
