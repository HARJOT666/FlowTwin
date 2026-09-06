package com.flowtwin.ingestion;

import com.flowtwin.model.PatientEvent;
import com.flowtwin.repository.PatientEventRepository;
import com.flowtwin.service.TwinStateService;
import com.flowtwin.ws.TwinBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PatientEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PatientEventConsumer.class);

    private final TwinStateService twin;
    private final PatientEventRepository repository;
    private final TwinBroadcaster broadcaster;

    public PatientEventConsumer(TwinStateService twin,
                                PatientEventRepository repository,
                                TwinBroadcaster broadcaster) {
        this.twin = twin;
        this.repository = repository;
        this.broadcaster = broadcaster;
    }

    @KafkaListener(topics = "${flowtwin.kafka.topic}")
    public void onEvent(PatientEventMessage msg) {
        try {
            Instant when = Instant.ofEpochMilli(msg.occurredAtEpochMs());
            twin.apply(msg);
            repository.save(new PatientEvent(msg.patientId(), msg.type(), msg.zone(), msg.acuity(), when));
            broadcaster.broadcastState(twin.snapshot());
        } catch (Exception e) {
            log.error("Failed to process event {}: {}", msg.eventId(), e.getMessage());
        }
    }
}
