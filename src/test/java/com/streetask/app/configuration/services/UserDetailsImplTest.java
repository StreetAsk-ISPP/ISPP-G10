package com.streetask.app.configuration.services;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.streetask.app.user.Authorities;
import com.streetask.app.user.User;

class UserDetailsImplTest {

    @Test
    void build_shouldHandleNullTokenVersion() {
        UUID id = UUID.randomUUID();
        Authorities authority = new Authorities();
        authority.setAuthority("USER");

        User user = new User();
        user.setId(id);
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setAuthority(authority);
        user.setTokenVersion(null);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        assertThat(userDetails.getTokenVersion()).isEqualTo(0L);
    }

    @Test
    void build_shouldHandleExistingTokenVersion() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setTokenVersion(5L);

        Authorities auth = new Authorities();
        auth.setAuthority("USER");
        user.setAuthority(auth);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        assertThat(userDetails.getTokenVersion()).isEqualTo(5L);
    }

    @Test
    void equals_shouldCoverAllBranches() {
        UUID id1 = UUID.randomUUID();
        UserDetailsImpl user1 = new UserDetailsImpl(id1, "u1", "p1", Collections.emptyList());
        UserDetailsImpl user1Copy = new UserDetailsImpl(id1, "u1", "p1", Collections.emptyList());
        UserDetailsImpl user2 = new UserDetailsImpl(UUID.randomUUID(), "u2", "p2", Collections.emptyList());

        assertThat(user1.equals(user1)).isTrue();
        assertThat(user1.equals("not a user")).isFalse();
        assertThat(user1.equals(null)).isFalse();
        assertThat(user1.equals(user1Copy)).isTrue();
        assertThat(user1.equals(user2)).isFalse();
    }

    @Test
    void hashCode_shouldBeBasedOnId() {
        UUID id = UUID.randomUUID();
        UserDetailsImpl user = new UserDetailsImpl(id, "u", "p", Collections.emptyList());

        int expectedHashCode = java.util.Objects.hash(id);
        assertThat(user.hashCode()).isEqualTo(expectedHashCode);
    }

    @Test
    void shouldReturnDefaultValues() {
        UserDetailsImpl user = new UserDetailsImpl(UUID.randomUUID(), "u", "p", Collections.emptyList());

        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getAuthorities()).isEmpty();
    }
}