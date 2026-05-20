package com.streetask.app.event;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.streetask.app.model.EventAttendance;

public interface EventAttendanceRepository extends CrudRepository<EventAttendance, UUID> {

        @Query("select ea from EventAttendance ea where ea.regularUser.id = :regularUserId and ea.event.id = :eventId")
        Optional<EventAttendance> findByRegularUserIdAndEventId(@Param("regularUserId") UUID regularUserId,
                        @Param("eventId") UUID eventId);

        @Query("select count(ea) from EventAttendance ea where ea.event.id = :eventId and ea.isAttending = true")
        long countAttendingByEventId(@Param("eventId") UUID eventId);

        @Query("select ea from EventAttendance ea join fetch ea.regularUser ru "
                        + "where ea.event.id = :eventId and ea.isAttending = true order by ea.confirmedAt asc")
        List<EventAttendance> findAttendingByEventId(@Param("eventId") UUID eventId);

        @Modifying
        @Query("delete from EventAttendance ea where ea.event.id = :eventId")
        void deleteByEventId(@Param("eventId") UUID eventId);

        @Query("select ea from EventAttendance ea where ea.event.id = :eventId")
        List<EventAttendance> findByEventId(@Param("eventId") UUID eventId);

        @Query("select ea.event.id from EventAttendance ea where ea.regularUser.id = :regularUserId and ea.isAttending = true")
        List<UUID> findAttendingEventIdsByRegularUserId(@Param("regularUserId") UUID regularUserId);
}