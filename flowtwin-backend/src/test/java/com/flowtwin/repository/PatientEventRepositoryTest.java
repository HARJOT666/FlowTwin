package com.flowtwin.repository;

import com.flowtwin.model.Acuity;
import com.flowtwin.model.EventType;
import com.flowtwin.model.PatientEvent;
import com.flowtwin.model.Zone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real PostgreSQL via Testcontainers (Boot 4.0 dropped @DataJpaTest, verified against the
 * actual dependency jars - no test-slice replacement exists, so this wires a narrow
 * JPA-only context instead of the full application). Covers Phase 8: connection,
 * persistence, retrieval, and the eventId uniqueness constraint that makes ingestion
 * idempotent under redelivery from whatever upstream feeds it (WebSocket ingestion included).
 */
@Testcontainers
@SpringBootTest(classes = PatientEventRepositoryTest.JpaOnlyConfig.class)
@Transactional
class PatientEventRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Configuration
    @EnableAutoConfiguration
    @EntityScan("com.flowtwin.model")
    @EnableJpaRepositories("com.flowtwin.repository")
    static class JpaOnlyConfig { }

    @Autowired
    private PatientEventRepository repository;

    @Test
    void savesAndRetrievesEventsInTimeWindow() {
        Instant now = Instant.now();
        repository.save(new PatientEvent("evt-1", "P-1", EventType.PATIENT_ARRIVED, Zone.TRIAGE, Acuity.L3, now));
        repository.save(new PatientEvent("evt-2", "P-2", EventType.PATIENT_ARRIVED, Zone.TRIAGE, Acuity.L2, now.plusSeconds(60)));

        var results = repository.findByOccurredAtBetweenOrderByOccurredAtAsc(
                now.minusSeconds(1), now.plusSeconds(120));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getPatientId()).isEqualTo("P-1");
    }

    @Test
    void existsByEventIdDetectsAlreadyPersistedEvent() {
        repository.save(new PatientEvent("evt-dup", "P-1", EventType.PATIENT_ARRIVED, Zone.TRIAGE, Acuity.L3, Instant.now()));

        assertThat(repository.existsByEventId("evt-dup")).isTrue();
        assertThat(repository.existsByEventId("evt-unknown-" + System.nanoTime())).isFalse();
    }

    @Test
    void duplicateEventIdViolatesUniqueConstraint() {
        Instant now = Instant.now();
        String eventId = "evt-dup-constraint-" + System.nanoTime();
        repository.saveAndFlush(new PatientEvent(eventId, "P-1", EventType.PATIENT_ARRIVED, Zone.TRIAGE, Acuity.L3, now));

        assertThatThrownBy(() -> repository.saveAndFlush(
                new PatientEvent(eventId, "P-2", EventType.PATIENT_ARRIVED, Zone.TRIAGE, Acuity.L1, now)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void countByTypeAndOccurredAtBetweenSupportsArrivalWindowFeature() {
        Instant now = Instant.now().plusSeconds(3600); // isolate window from other tests' inserts
        repository.save(new PatientEvent("evt-w1-" + System.nanoTime(), "P-1", EventType.PATIENT_ARRIVED, Zone.TRIAGE, Acuity.L3, now));
        repository.save(new PatientEvent("evt-w2-" + System.nanoTime(), "P-2", EventType.PATIENT_ARRIVED, Zone.TRIAGE, Acuity.L2, now));
        repository.save(new PatientEvent("evt-w3-" + System.nanoTime(), "P-1", EventType.TRIAGED, Zone.TREATMENT, Acuity.L3, now));

        long arrivals = repository.countByTypeAndOccurredAtBetween(
                EventType.PATIENT_ARRIVED, now.minusSeconds(1), now.plusSeconds(1));

        assertThat(arrivals).isEqualTo(2);
    }
}
