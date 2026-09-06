package com.flowtwin.ai;

import com.flowtwin.ingestion.PatientEventMessage;
import com.flowtwin.model.EventType;
import com.flowtwin.model.Zone;
import com.flowtwin.service.TwinStateService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TwinStateServiceTest {
    @Test void noArrivalsIsZeroAndRecordedArrivalsAreCounted() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenReturn(mock(ValueOperations.class));
        TwinStateService twin = new TwinStateService(redis);
        assertThat(twin.snapshot().observedArrivalRatePerHour()).isZero();
        twin.apply(new PatientEventMessage("event", "patient", EventType.PATIENT_ARRIVED,
                Zone.TRIAGE, null, System.currentTimeMillis()));
        assertThat(twin.snapshot().observedArrivalRatePerHour()).isEqualTo(1);
    }
}
