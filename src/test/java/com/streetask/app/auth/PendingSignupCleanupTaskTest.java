package com.streetask.app.auth;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class PendingSignupCleanupTaskTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void cleanupStalePendingUsersShouldDeleteWhenStaleUsersExist() {
        User user = new User();
        when(userRepository.findStalePendingBasicUsers(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of(user));

        PendingSignupCleanupTask task = new PendingSignupCleanupTask(userRepository);
        task.cleanupStalePendingUsers();

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository, times(1)).findStalePendingBasicUsers(cutoffCaptor.capture());
        verify(userRepository, times(1)).deleteAll(List.of(user));
    }

    @Test
    void cleanupStalePendingUsersShouldNotDeleteWhenNoStaleUsers() {
        when(userRepository.findStalePendingBasicUsers(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of());

        PendingSignupCleanupTask task = new PendingSignupCleanupTask(userRepository);
        task.cleanupStalePendingUsers();

        verify(userRepository, times(1))
                .findStalePendingBasicUsers(org.mockito.ArgumentMatchers.any(LocalDateTime.class));
        verify(userRepository, never()).deleteAll(org.mockito.ArgumentMatchers.any());
    }
}
