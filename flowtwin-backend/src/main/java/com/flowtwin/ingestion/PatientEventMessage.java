package com.flowtwin.ingestion;

import com.flowtwin.model.Acuity;
import com.flowtwin.model.EventType;
import com.flowtwin.model.Zone;

/**
 * Wire format on the Kafka topic. Timestamp kept as epoch millis to avoid
 * JSR-310 serializer setup on the Kafka (de)serializers.
 */
public record PatientEventMessage(
        String eventId,
        String patientId,
        EventType type,
        Zone zone,
        Acuity acuity,
        long occurredAtEpochMs
) {}
