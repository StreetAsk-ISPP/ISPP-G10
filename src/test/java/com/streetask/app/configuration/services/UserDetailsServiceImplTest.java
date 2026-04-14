package com.streetask.app.configuration.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(identifier));
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
