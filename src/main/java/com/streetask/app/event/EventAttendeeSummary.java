package com.streetask.app.event;

import java.util.UUID;

public record EventAttendeeSummary(
        UUID id,
        String userName,
        String firstName,
        String lastName,
        String email,
        String profilePhoto) {
}