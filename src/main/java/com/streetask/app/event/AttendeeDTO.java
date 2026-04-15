package com.streetask.app.event;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class AttendeeDTO {
	private UUID userId;
	private String userName;
	private String email;
	private Boolean isAttending;
	private LocalDateTime confirmedAt;
}
