package com.flowtwin.service;

import com.flowtwin.ingestion.PatientEventMessage;
import com.flowtwin.model.PatientEvent;
import com.flowtwin.repository.PatientEventRepository;
import com.flowtwin.ws.TwinBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Core patient-event processing pipeline: update the live twin, persist the event,
 * then broadcast the fresh snapshot over WebSocket.
 *
 * This is the single entry point for ingesting a {@link PatientEventMessage}, independent of
 * the transport that produced it. Today the in-process {@code DemoEventSimulator} calls
 * {@link #process(PatientEventMessage)} directly; when Kafka is reintroduced later, a Kafka
 * consumer can call this same method so no business logic has to move again.
 */
@Service
public class EventProcessingService {

    private static final Logger log = LoggerFactory.getLogger(EventProcessingService.class);

    private final TwinStateService twin;
    private final PatientEventRepository repository;
    private final TwinBroadcaster broadcaster;

    public EventProcessingService(TwinStateService twin,
                                  PatientEventRepository repository,
                                  TwinBroadcaster broadcaster) {
        this.twin = twin;
        this.repository = repository;
        this.broadcaster = broadcaster;
    }

    public void process(PatientEventMessage msg) {
        try {
            if (repository.existsByEventId(msg.eventId())) {
                log.debug("Duplicate event {} ignored", msg.eventId());
                return;
            }
            Instant when = Instant.ofEpochMilli(msg.occurredAtEpochMs());
            twin.apply(msg);
            repository.save(new PatientEvent(msg.eventId(), msg.patientId(), msg.type(), msg.zone(), msg.acuity(), when));
            broadcaster.broadcastState(twin.snapshot());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.debug("Duplicate event {} rejected by unique constraint", msg.eventId());
        } catch (Exception e) {
            log.error("Failed to process event {}: {}", msg.eventId(), e.getMessage());
        }
    }
}
