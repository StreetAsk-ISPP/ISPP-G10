package com.streetask.app.configuration.services;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.streetask.app.user.Authorities;
import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void shouldLoadUserByUuidIdentifierWhenEmailAndUsernameLookupsMiss() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "user@example.com", "streetask_user");

        when(userRepository.findByEmailIgnoreCase(userId.toString())).thenReturn(Optional.empty());
        when(userRepository.findByUserNameIgnoreCase(userId.toString())).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(userId.toString());

        assertEquals("user@example.com", userDetails.getUsername());
        verify(userRepository).findById(userId);
    }

    @Test
    void shouldThrowWhenIdentifierDoesNotMatchAnyUser() {
        String identifier = "missing@example.com";
        when(userRepository.findByEmailIgnoreCase(identifier)).thenReturn(Optional.empty());
        when(userRepository.findByUserNameIgnoreCase(identifier)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(identifier));
        assertThat(exception.getMessage()).contains("User Not Found with email, username, or id: ");
    }

    @Test
    void shouldThrowWhenIdentifierIsNull() {
        when(userRepository.findByEmailIgnoreCase("")).thenReturn(Optional.empty());
        when(userRepository.findByUserNameIgnoreCase("")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(null));
        assertThat(exception.getMessage()).contains("User Not Found with email, username, or id: ");
    }

    private User buildUser(UUID id, String email, String username) {
        Authorities authority = new Authorities();
        authority.setAuthority("USER");

        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUserName(username);
        user.setPassword("encoded-password");
        user.setFirstName("Street");
        user.setLastName("Ask");
        user.setAuthority(authority);
        return user;
    }
}
