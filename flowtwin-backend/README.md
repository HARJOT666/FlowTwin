# FlowTwin — Backend

Spring Boot 4.0 / Java 17 backend for FlowTwin, a real-time digital twin for ED patient flow.
This is a **skeleton**: it boots, self-feeds demo data, runs what-if simulations, and narrates
them with an LLM. Flesh out the `TODO`s to harden it.

## Run it

```bash
cp .env.example .env      # put your LLM_API_KEY in here
docker compose up --build
```

That starts Postgres, Redis, and the backend. The built-in **DemoEventSimulator**
begins emitting patient-flow events immediately, so the twin populates on its own.

Run the app alone (needs Postgres/Redis reachable, e.g. `docker compose up postgres redis`):

```bash
mvn spring-boot:run
```

> No Maven wrapper is bundled — use a local Maven/IDE, or add one with `mvn wrapper:wrapper`.

## Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/twin/state` | Live twin snapshot |
| GET | `/api/twin/metrics` | Live metrics |
| POST | `/api/scenarios` | Run a what-if; returns projected impact + AI narration |
| GET | `/api/scenarios/{id}` | Fetch a stored scenario |
| WS | `/ws/twin` (STOMP) | Subscribe `/topic/twin` (state) and `/topic/twin/insight` (scenarios) |

Example:

```bash
curl -X POST http://localhost:8080/api/scenarios \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Extra triage nurse",
        "changes": [ { "type": "STAFF", "role": "NURSE", "delta": 1 } ],
        "horizonHours": 4
      }'
```

## Layout

```
com.flowtwin
├── config/       WebSocket (STOMP) + LLM properties
├── model/        domain enums + PatientEvent JPA entity
├── ingestion/    patient-event message DTO (fed to EventProcessingService)
├── twin/         live twin-state service (in-memory + Redis) + REST
├── simulation/   discrete-event simulation engine (arrival -> triage -> bed -> discharge)
├── scenario/     what-if orchestrator, metrics/deltas, persistence, REST
├── narration/    grounded prompt builder, LLM service (cache + fallback), OpenAI-compatible provider
├── ws/           WebSocket broadcaster
└── simulator/    built-in demo event generator
```

## Where the interesting code is

- **`simulation/SimulationEngine`** — a real (small) discrete-event queueing simulation.
- **`scenario/ScenarioOrchestrator`** — snapshot twin → run baseline + scenario → delta → narrate → persist → broadcast.
- **`narration/PromptBuilder`** — the grounding guardrail: the LLM only ever sees the simulation's own numbers.

## Known TODOs / caveats

- Build and test with Java 17+ and Maven 3.9: `mvn test`, then `mvn clean package`.
- Time-windowed scenario changes (`from`/`to`) are parsed but applied for the whole horizon — see `ScenarioOrchestrator.applyChanges`.
- Service-time and arrival assumptions are constants/rough estimates — replace with values learned from history.
- Only an OpenAI-compatible provider is implemented; add Gemini/Claude providers behind `NarrationProvider`.
- Redis stores headline metrics only; full snapshot serialization is a TODO.
- Spring Boot 4 notes: Jackson 3 is the default; JUnit 4 is dropped (use JUnit 5); check security defaults before exposing endpoints.

## FlowTwin Predictive AI

This is a hackathon operational decision-support prototype and is not a clinically validated medical device.
Risk, confidence and effectiveness scores are explainable heuristics, not calibrated probabilities or clinical accuracy claims.
No trained model, Python service, new infrastructure, or LLM API key is required for predictions.

### Integration with Harjot's backend

`EventProcessingService -> PatientEventRepository / TwinStateService -> TwinState -> ArrivalForecastService -> BottleneckPredictionService -> AiInsightService`

This branch includes Harjot's backend update `d1aff94`, which replaced Kafka with direct event processing.
The AI layer follows that updated ingestion architecture; it does not reintroduce Kafka or replace ingestion.

The existing `TwinState` remains the source of truth. The AI package is `com.flowtwin.ai`.
The existing scenario flow remains `snapshot -> baseline/scenario SimulationEngine -> Metrics/delta -> RecommendationEngine -> NarrationService -> save -> broadcast`.
Forecast and bottleneck context are computed from that same snapshot and baseline metrics for narration;
they do not silently replace the simulation arrival rate. Existing timeline bottleneck detection is retained.

### Endpoints

| Method | Path | Response |
|---|---|---|
| GET | `/api/ai/insights` | Timestamp, current TwinState, forecast, predicted bottleneck |
| GET | `/api/ai/forecast` | Forecast object only |
| GET | `/api/ai/summary` | Deterministic summary, surgeRisk, recommendedFocus |
| POST | `/api/scenarios` | Existing id/result/narration plus additive recommendation |

Existing twin/scenario endpoints and WebSocket topics remain available. Scenario POST and its WebSocket
broadcast carry the same recommendation. `GET /api/scenarios/{id}` preserves the existing reduced persisted
entity (name, P90 values, narration, recommendations text, timestamp); it does not persist the new score or
full simulation. No database schema migration is introduced. The old three-argument ScenarioResponse
constructor and one-argument narration/prompt methods remain available.

### Short-term arrival forecasting and surge risk

`ArrivalForecastService` counts only `PATIENT_ARRIVED` events in four consecutive, non-overlapping hourly
windows ending at the request timestamp. Windows include their start and exclude their end; future events
are excluded. Counts are queried in the database without loading patient lists. History needs at least
eight arrivals in those windows and an arrival at or before the start of the four-hour window. This is a
minimal coverage proxy: ingestion gaps cannot be distinguished from true quiet hours in the current schema.

For sufficient history, with hourly counts oldest to newest:

- EWMA starts at the first count and uses alpha 0.5 for each later hour.
- `baseline = total / 4`; `recent = mean(last two hours)`; `earlier = mean(first two hours)`.
- Initial rate = `0.5 * EWMA + 0.3 * recent + 0.2 * observedArrivalRatePerHour`.
- Hourly slope = `(recent - earlier) / 2`, capped to +/-25% of baseline.
- Each future hour adds `slope * 0.75^(hour-1)` to the preceding rate, floored at zero.
- Cumulative expectations at 1, 2 and 4 hours are rounded to whole arrivals.
- Trend is RISING/FALLING when slope exceeds +/-`max(0.5, 5% of baseline)`, otherwise STABLE.

Sparse history uses the current observed arrival rate unchanged, with STABLE indicating no established
trend. Sources are `HISTORY_BLEND`, `OBSERVED_RATE`, or `HISTORY_UNAVAILABLE` (database query failure).
`historicalArrivals` and `riskFactors` expose calculation evidence. The twin's previous hardcoded 12/hour
fallback is removed: no recorded arrivals now means zero. Its snapshot/arrival-rate reads are synchronized
with ingestion. The existing built-in demo event generator remains the optional source of synthetic events;
the AI itself never creates synthetic history or random predictions.

Surge risk = `0.30 * arrivalGrowth + 0.25 * queuePressure + 0.25 * bedOccupancy + 0.20 * triageDemand`,
rounded to three decimals. Each factor is clamped to [0,1]:

- arrivalGrowth = `(nextHourArrivals - baseline) / max(1, baseline)`.
- queuePressure = `triageQueue / max(1, nurses * 2)`.
- bedOccupancy = `bedsOccupied / max(1, beds)`.
- triageDemand = `nextHourArrivals / max(1, nurses * 10)`.

This uses current queue pressure, not an invented queue-growth measurement. All forecasts and risk remain
available without an LLM. Database fallback helps an already running service; initial application startup
still requires its configured database.

### Predictive bottleneck detection

`BottleneckPredictionService` compares resource pressure ratios for the next hour. Assumptions are six
minutes per triage (10 arrivals/nurse/hour), 90 minutes per occupied bed, four treatment patients per nurse,
and six patients per doctor/hour. These are prototype assumptions, not measured hospital capacities.

- TRIAGE: max of `queue / (2*nurses)` and `(queue + nextHourArrivals) / (10*nurses)`.
- BEDS: max of `occupied / (0.8*beds)` and `(occupied + max(0, arrivals - occupied/1.5)) / beds`.
- DOCTORS: `(patientsInDept + arrivals) / (6*doctors)`.
- TREATMENT: `max(0, patientsInDept - queue) / (4*nurses)`.

The highest ratio selects the zone. Ratios below 0.85 return NONE/LOW; 0.85 to below 1.1 is MEDIUM;
1.1 to below 2 is HIGH; 2+ is CRITICAL. Ratios are capped at 10; positive demand with zero capacity is
assigned 10 and zero demand with zero capacity is 0. Ties use TRIAGE, BEDS, DOCTORS, TREATMENT order.
Optional baseline simulation peak queue and bed utilization can raise the matching pressure ratios.
Confidence is an evidence-strength label: 0.65 for history blend, 0.40 for observed fallback, plus 0.10
when simulation evidence is supplied. The response includes the pressure ratios and a deterministic reason.

### Simulation-based recommendation ranking

`RecommendationEngine.rankScenario` scores only fields present in existing Metrics:
P90 wait (weight 40), average triage wait (30), peak triage queue (20), average bed wait (10).
Each contribution is `weight * clamp((baseline - scenario) / max(1, baseline), -1, 1)`.
The sum is rounded to one decimal, with a signed range of -100 to +100. Positive is beneficial; negative
is harmful; zero is neutral. Impact is NEGATIVE below zero, NONE at zero, LOW above zero to below 10,
MEDIUM from 10 to below 30, and HIGH from 30. Mixed improvements/regressions are reflected in the sum.
The response includes each weighted contribution and the actual baseline/scenario P90 values.
`rankScenarios` sorts already simulated alternatives by descending score, preserving input order for ties.

No throughput is fabricated, and lower bed utilization alone is not automatically rewarded.
Scenario integration rejects apparently improved zero waits when all nurses or beds are removed with
positive arrival demand. Such a blocked-capacity scenario receives -100/NEGATIVE.

Current simulation limitations are exposed with every score: it starts empty, does not use doctors in
service scheduling, and excludes patients still waiting at the horizon from completed wait metrics.
Consequently, compare equal horizons and assumptions; doctor-only changes can score neutral even when
the separate pressure heuristic identifies doctor constraints. Scores do not measure clinical outcomes,
cost, or workforce feasibility. The existing simulation engine is not replaced or rewritten.

Example: if all four scored wait/queue metrics improve by half, the recommendation is:

```json
{
  "score": 50.0,
  "impact": "HIGH",
  "primaryReason": "Weighted wait and queue improvement is 50.0 points; projected P90 wait changes from 60.0 to 30.0 minutes.",
  "weightedContributions": {
    "p90Wait": 20.0,
    "averageTriageWait": 15.0,
    "peakTriageQueue": 10.0,
    "averageBedWait": 5.0
  },
  "limitations": [
    "Prototype simulation starts empty and does not model doctor capacity or throughput.",
    "Wait metrics exclude patients still waiting at the horizon; compare equal horizons and assumptions.",
    "Scores compare modeled patient flow, not clinical outcomes or intervention cost."
  ]
}
```

### Grounded LLM narration

The existing PromptBuilder, NarrationService, NarrationProvider, OpenAiNarrationProvider and
TemplatedFallback are reused. Optional context contains only the current state, calculated forecast,
resource pressure and effectiveness score. The prompt instructs the LLM to summarize supplied numbers,
not calculate metrics or rank actions, and to treat scenario names as untrusted labels.
This is prompt grounding, not a formal guarantee of model output correctness; deterministic numerical API
fields remain authoritative. No patient identifiers or raw event records are sent to the LLM.

Redis keys now hash the exact system/user prompt with SHA-256, avoiding reuse across different calculated
values. Request timestamps are omitted from the prompt so equivalent evidence can reuse the cache.
Redis read/write failures are tolerated; missing API keys skip the HTTP request and retain the existing
templated scenario fallback. The optional summary endpoint is always deterministic.

### Example insights response

Illustrative empty-state response with the existing default resource counts, no arrival history and demo
ingestion disabled. The timestamp is illustrative; numerical fields follow the calculations above.

```json
{
  "timestamp": "2026-09-06T10:00:00Z",
  "currentState": {
    "patientsInDept": 0, "triageQueue": 0, "bedsOccupied": 0,
    "nurses": 4, "beds": 20, "doctors": 3, "observedArrivalRatePerHour": 0.0
  },
  "forecast": {
    "currentArrivalRatePerHour": 0.0,
    "predictedArrivalsNextHour": 0,
    "predictedArrivalsNext2Hours": 0,
    "predictedArrivalsNext4Hours": 0,
    "trend": "STABLE", "surgeRisk": 0.0,
    "source": "OBSERVED_RATE", "historicalArrivals": 0,
    "riskFactors": {"arrivalGrowth": 0.0, "queuePressure": 0.0, "bedOccupancy": 0.0, "triageDemand": 0.0}
  },
  "bottleneck": {
    "zone": "NONE", "severity": "LOW", "confidence": 0.4,
    "reason": "No resource exceeds the prototype pressure threshold in the next hour.",
    "pressureRatios": {"TRIAGE": 0.0, "BEDS": 0.0, "DOCTORS": 0.0, "TREATMENT": 0.0}
  }
}
```

### Tests and handoff

Run `mvn test` and `mvn clean package` from `flowtwin-backend/` with Java 17+ and Maven 3.9.
Tests cover forecast trends/sparse/zero/outage data, event boundaries/type filtering, bottleneck zones,
beneficial/neutral/harmful ranking, zero-capacity safeguards, narration/cache fallback and existing/new REST
contracts. Integration tests use H2 only in test scope, real Spring MVC/services/JPA, and mocks for external
Redis/LLM/WebSocket delivery. Production remains PostgreSQL/Redis with Harjot's direct ingestion. Live infrastructure and
external LLM availability need their own environment smoke test.

For Harjot: use this branch for integration, retain the existing environment settings, and let the frontend
call `/api/ai/insights`. Read `recommendation` from scenario POST/WebSocket responses. There are no required
new environment variables or model downloads. Do not treat score/risk/confidence as clinical probabilities.

Upstream configuration note: the current application defaults to PostgreSQL credentials `postgres`, while
the supplied Compose database uses `flowtwin`. Set `POSTGRES_USER` and `POSTGRES_PASSWORD` to match your
database when launching the backend (including explicitly passing them into the backend container).
