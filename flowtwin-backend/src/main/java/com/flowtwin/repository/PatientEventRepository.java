
package com.flowtwin.repository;

import com.flowtwin.model.Acuity;
import com.flowtwin.model.EventType;
import com.flowtwin.model.PatientEvent;
import com.flowtwin.model.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PatientEventRepository extends JpaRepository<PatientEvent, UUID> {

    List<PatientEvent> findByOccurredAtBetweenOrderByOccurredAtAsc(Instant from, Instant to);

    /** Idempotency guard: check before persisting so redelivered/duplicate messages are ignored. */
    boolean existsByEventId(String eventId);

    /** ML feature: e.g. arrivals in last 15/30/60 min = countByTypeAndOccurredAtBetween(PATIENT_ARRIVED, now.minus(...), now). */
    long countByTypeAndOccurredAtBetween(EventType type, Instant from, Instant to);

    long countByZoneAndOccurredAtBetween(Zone zone, Instant from, Instant to);

    /** ML feature: acuity distribution of arrivals in a time window. */
    @Query("select e.acuity as acuity, count(e) as total from PatientEvent e " +
           "where e.type = com.flowtwin.model.EventType.PATIENT_ARRIVED and e.occurredAt between :from and :to " +
           "group by e.acuity")
    List<AcuityCount> acuityDistribution(@Param("from") Instant from, @Param("to") Instant to);

    interface AcuityCount {
        Acuity getAcuity();
        long getTotal();
    }
}
