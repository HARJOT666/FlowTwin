package com.flowtwin.ai;

import com.flowtwin.model.*;
import com.flowtwin.narration.NarrationProvider;
import com.flowtwin.repository.PatientEventRepository;
import com.flowtwin.repository.ScenarioRepository;
import com.flowtwin.service.TwinStateService;
import com.flowtwin.ws.TwinBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real Spring MVC, services and JPA; only external messaging/cache/LLM are mocked. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "flowtwin.simulator.enabled=false"
})
class AiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired PatientEventRepository events;
    @Autowired ScenarioRepository scenarios;
    @Autowired TwinStateService twin;
    @MockitoBean StringRedisTemplate redis;
    @MockitoBean NarrationProvider provider;
    @MockitoBean TwinBroadcaster broadcaster;
    private MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        when(redis.opsForValue()).thenReturn(mock(ValueOperations.class));
        when(provider.generate(anyString(), anyString())).thenThrow(new IllegalStateException("no API key"));
        events.deleteAll();
        scenarios.deleteAll();
    }

    @Test void newEndpointsAndExistingTwinEndpointWorkWithoutLlm() throws Exception {
        mvc.perform(get("/api/ai/insights")).andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.currentState.beds").value(20))
                .andExpect(jsonPath("$.forecast.source").value("OBSERVED_RATE"))
                .andExpect(jsonPath("$.forecast.predictedArrivalsNext4Hours").value(0))
                .andExpect(jsonPath("$.bottleneck.zone").value("NONE"));
        mvc.perform(get("/api/ai/forecast")).andExpect(status().isOk())
                .andExpect(jsonPath("$.surgeRisk").value(0));
        mvc.perform(get("/api/ai/summary")).andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedFocus").value("NONE"));
        mvc.perform(get("/api/twin/state")).andExpect(status().isOk())
                .andExpect(jsonPath("$.observedArrivalRatePerHour").value(0));
        verifyNoInteractions(provider);
    }

    @Test void existingScenarioPostPersistsBroadcastsAndAddsRecommendation() throws Exception {
        mvc.perform(post("/api/scenarios").contentType("application/json").content("""
                {"name":"Extra triage nurse","changes":[{"type":"STAFF","role":"NURSE","delta":1}],"horizonHours":4}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.result.baseline").exists())
                .andExpect(jsonPath("$.result.scenario").exists())
                .andExpect(jsonPath("$.result.delta").exists())
                .andExpect(jsonPath("$.narration.source").value("fallback"))
                .andExpect(jsonPath("$.recommendation.score").value(0));
        assertThat(scenarios.count()).isEqualTo(1);
        Long id = scenarios.findAll().get(0).getId();
        mvc.perform(get("/api/scenarios/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Extra triage nurse"));
        mvc.perform(get("/api/scenarios/999999")).andExpect(status().isNotFound());
        verify(broadcaster).broadcastInsight(argThat(value -> value instanceof com.flowtwin.scenario.ScenarioResponse r
                && r.recommendation() != null));
        verify(provider).generate(contains("Do not calculate new numbers"), contains("Calculated scenario effectiveness"));
    }

    @Test void arrivalQueryFiltersTypesAndUsesHalfOpenBoundaries() {
        Instant from = Instant.parse("2026-09-06T09:00:00Z"), to = from.plusSeconds(3600);
        events.save(new PatientEvent("before", EventType.PATIENT_ARRIVED, Zone.TRIAGE, Acuity.L3, from.minusSeconds(1)));
        events.save(new PatientEvent("start", EventType.PATIENT_ARRIVED, Zone.TRIAGE, Acuity.L3, from));
        events.save(new PatientEvent("end", EventType.PATIENT_ARRIVED, Zone.TRIAGE, Acuity.L3, to));
        events.save(new PatientEvent("discharged", EventType.DISCHARGED, Zone.BED, Acuity.L3, from.plusSeconds(100)));
        assertThat(events.countByTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                EventType.PATIENT_ARRIVED, from, to)).isEqualTo(1);
        assertThat(events.findFirstByTypeOrderByOccurredAtAsc(EventType.PATIENT_ARRIVED).orElseThrow().getPatientId())
                .isEqualTo("before");
    }
}
