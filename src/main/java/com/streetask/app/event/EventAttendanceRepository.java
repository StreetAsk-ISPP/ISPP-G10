package com.streetask.app.event;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import com.streetask.app.model.EventAttendance;

public interface EventAttendanceRepository extends CrudRepository<EventAttendance, UUID> {
	List<EventAttendance> findByEventId(UUID eventId);
	List<EventAttendance> findByEventIdAndIsAttendingTrue(UUID eventId);
	long countByEventIdAndIsAttendingTrue(UUID eventId);
}
