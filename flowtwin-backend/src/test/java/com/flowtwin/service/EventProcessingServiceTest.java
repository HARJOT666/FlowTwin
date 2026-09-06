package com.flowtwin.service;

import com.flowtwin.ingestion.PatientEventMessage;
import com.flowtwin.model.Acuity;
import com.flowtwin.model.EventType;
import com.flowtwin.model.PatientEvent;
import com.flowtwin.model.Zone;
import com.flowtwin.repository.PatientEventRepository;
import com.flowtwin.twin.TwinState;
import com.flowtwin.ws.TwinBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Duplicate-delivery safety net: a redelivered eventId must not touch twin state, persistence, or broadcast. */
class EventProcessingServiceTest {

    private TwinStateService twin;
    private PatientEventRepository repository;
    private TwinBroadcaster broadcaster;
    private EventProcessingService service;

    @BeforeEach
    void setUp() {
        twin = mock(TwinStateService.class);
        repository = mock(PatientEventRepository.class);
        broadcaster = mock(TwinBroadcaster.class);
        service = new EventProcessingService(twin, repository, broadcaster);
        when(twin.snapshot()).thenReturn(new TwinState(0, 0, 0, 4, 20, 3, 12.0));
    }

    @Test
    void processesNewEventOnce() {
        when(repository.existsByEventId("evt-1")).thenReturn(false);
        PatientEventMessage msg = new PatientEventMessage(
                "evt-1", "P-1", EventType.PATIENT_ARRIVED, Zone.TRIAGE, Acuity.L3, System.currentTimeMillis());

        service.process(msg);

        verify(twin, times(1)).apply(msg);
        verify(repository, times(1)).save(any(PatientEvent.class));
        verify(broadcaster, times(1)).broadcastState(any());
    }

    @Test
    void ignoresDuplicateEventId() {
        when(repository.existsByEventId("evt-dup")).thenReturn(true);
        PatientEventMessage msg = new PatientEventMessage(
                "evt-dup", "P-1", EventType.PATIENT_ARRIVED, Zone.TRIAGE, Acuity.L3, System.currentTimeMillis());

        service.process(msg);

        verify(twin, never()).apply(any());
        verify(repository, never()).save(any());
        verify(broadcaster, never()).broadcastState(any());
    }
}
