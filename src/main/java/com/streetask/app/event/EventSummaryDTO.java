package com.streetask.app.event;

import java.time.LocalDateTime;
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
public class EventSummaryDTO {
	private UUID id;
	private String title;
	private String description;
	private EventCategory category;
	private String address;
	private Double latitude;
	private Double longitude;
	private LocalDateTime startsAt;
	private LocalDateTime endsAt;
	private Integer attendeeCount;
	private Boolean active;
	private LocalDateTime createdAt;
	private String creatorName;
	private UUID creatorId;
}
