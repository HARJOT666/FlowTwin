package com.flowtwin.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patient_event", indexes = @Index(name = "idx_event_time", columnList = "occurredAt"))
public class PatientEvent {

    @Id
    @GeneratedValue
    private UUID id;

    private String patientId;

    @Enumerated(EnumType.STRING)
    private EventType type;

    @Enumerated(EnumType.STRING)
    private Zone zone;

    @Enumerated(EnumType.STRING)
    private Acuity acuity;

    private Instant occurredAt;

    protected PatientEvent() { }

    public PatientEvent(String patientId, EventType type, Zone zone, Acuity acuity, Instant occurredAt) {
        this.patientId = patientId;
        this.type = type;
        this.zone = zone;
        this.acuity = acuity;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public String getPatientId() { return patientId; }
    public EventType getType() { return type; }
    public Zone getZone() { return zone; }
    public Acuity getAcuity() { return acuity; }
    public Instant getOccurredAt() { return occurredAt; }
}
