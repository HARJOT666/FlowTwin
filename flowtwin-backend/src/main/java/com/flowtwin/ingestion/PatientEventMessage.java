package com.flowtwin.ingestion;

import com.flowtwin.model.Acuity;
import com.flowtwin.model.EventType;
import com.flowtwin.model.Zone;

/**
 * Transport-agnostic patient-event message: the unit of ingestion consumed by
 * {@code EventProcessingService}. Timestamp kept as epoch millis so the record stays a
 * plain, easily (de)serialized DTO regardless of the input mechanism producing it.
 */
public record PatientEventMessage(
        String eventId,
        String patientId,
        EventType type,
        Zone zone,
        Acuity acuity,
        long occurredAtEpochMs
) {}
