package com.flowtwin.repository;

import com.flowtwin.model.PatientEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PatientEventRepository extends JpaRepository<PatientEvent, UUID> {
    List<PatientEvent> findByOccurredAtBetweenOrderByOccurredAtAsc(Instant from, Instant to);
}
