package com.flowtwin.service;

import com.flowtwin.ingestion.PatientEventMessage;
import com.flowtwin.model.Acuity;
import com.flowtwin.model.EventType;
import com.flowtwin.model.Zone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifies the live-twin -> Redis mirroring key convention (twin:triageQueue,
 * twin:bedsOccupied, twin:patientsInDept). Redis itself is mocked here - Postgres is the
 * source of truth for historical data, Redis only carries fast-read live state, so this
 * checks the write shape rather than requiring a running Redis server.
 */
class TwinStateServiceRedisTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private TwinStateService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        service = new TwinStateService(redis);
    }

    @Test
    void patientArrivalMirrorsExpectedKeysToRedis() {
        PatientEventMessage msg = new PatientEventMessage(
                "evt-1", "P-1", EventType.PATIENT_ARRIVED, Zone.TRIAGE, Acuity.L2, System.currentTimeMillis());

        service.apply(msg);

        verify(valueOps).set(eq("twin:triageQueue"), eq("1"));
        verify(valueOps).set(eq("twin:bedsOccupied"), eq("0"));
        verify(valueOps).set(eq("twin:patientsInDept"), eq("1"));
    }
}
