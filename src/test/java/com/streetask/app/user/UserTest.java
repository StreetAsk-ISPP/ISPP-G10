package com.streetask.app.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("Coverage: hasAnyAuthority branches (empty, no match, match)")
    void coverage_hasAnyAuthority() {
        RegularUser user = new RegularUser();
        Authorities authorities = new Authorities();
        authorities.setAuthority("USER");
        user.setAuthority(authorities);

        assertFalse(user.hasAnyAuthority());

        assertFalse(user.hasAnyAuthority("ADMIN", "MODERATOR"));

        assertTrue(user.hasAnyAuthority("ADMIN", "USER"));
    }
}