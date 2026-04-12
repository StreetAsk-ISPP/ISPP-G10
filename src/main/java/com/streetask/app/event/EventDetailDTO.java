package com.streetask.app.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.streetask.app.model.enums.EventCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class EventDetailDTO {
	private UUID id;
	private String title;
	private String description;
	private EventCategory category;
	private String address;
	private Double latitude;
	private Double longitude;
	private LocalDateTime startsAt;
	private LocalDateTime endsAt;
	private Integer totalAttendeeCount;
	private Integer confirmedAttendeeCount;
	private Boolean active;
	private Boolean featured;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private String creatorName;
	private UUID creatorId;
	private List<AttendeeDTO> attendees;
}
