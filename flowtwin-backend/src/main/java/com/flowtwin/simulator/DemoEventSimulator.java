package com.flowtwin.simulator;

import com.flowtwin.ingestion.PatientEventMessage;
import com.flowtwin.model.Acuity;
import com.flowtwin.model.EventType;
import com.flowtwin.model.Zone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import java.util.UUID;

/**
 * Built-in demo data source: pushes synthetic patient-flow events to Kafka so the twin
 * comes alive with zero external dependencies. Disable with flowtwin.simulator.enabled=false.
 */
@Component
public class DemoEventSimulator {

    private final KafkaTemplate<String, PatientEventMessage> kafka;
    private final String topic;
    private final boolean enabled;
    private final Random rng = new Random();
    private final Deque<String> inTriage = new ArrayDeque<>();
    private final Deque<String> inBed = new ArrayDeque<>();

    public DemoEventSimulator(KafkaTemplate<String, PatientEventMessage> kafka,
                              @Value("${flowtwin.kafka.topic}") String topic,
                              @Value("${flowtwin.simulator.enabled:true}") boolean enabled) {
        this.kafka = kafka;
        this.topic = topic;
        this.enabled = enabled;
    }

    @Scheduled(fixedRate = 1500)
    public void tick() {
        if (!enabled) return;
        double roll = rng.nextDouble();
        if (roll < 0.45) {
            String pid = "P-" + UUID.randomUUID().toString().substring(0, 8);
            inTriage.addLast(pid);
            emit(pid, EventType.PATIENT_ARRIVED, Zone.TRIAGE, randomAcuity());
        } else if (roll < 0.7 && !inTriage.isEmpty()) {
            String pid = inTriage.pollFirst();
            emit(pid, EventType.TRIAGED, Zone.TREATMENT, null);
            inBed.addLast(pid);
            emit(pid, EventType.BED_ASSIGNED, Zone.BED, null);
        } else if (!inBed.isEmpty()) {
            String pid = inBed.pollFirst();
            emit(pid, EventType.DISCHARGED, Zone.BED, null);
        }
    }

    private void emit(String pid, EventType type, Zone zone, Acuity acuity) {
        kafka.send(topic, pid, new PatientEventMessage(
                UUID.randomUUID().toString(), pid, type, zone, acuity, System.currentTimeMillis()));
    }

    private Acuity randomAcuity() {
        Acuity[] all = Acuity.values();
        return all[rng.nextInt(all.length)];
    }
}
