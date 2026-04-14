package com.streetask.app.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.streetask.app.user.User;

class PasswordResetTokenTest {

    @Test
    void shouldStoreAndReturnAllFields() {
        PasswordResetToken token = new PasswordResetToken();
        User user = new User();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
        LocalDateTime usedAt = LocalDateTime.now();

        token.setUser(user);
        token.setToken("token-value");
        token.setExpiresAt(expiresAt);
        token.setUsedAt(usedAt);

        assertThat(token.getUser()).isEqualTo(user);
        assertThat(token.getToken()).isEqualTo("token-value");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.getUsedAt()).isEqualTo(usedAt);
    }
}
