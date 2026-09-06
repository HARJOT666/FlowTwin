# FlowTwin Backend

Spring Boot 4 / Java 17 backend for FlowTwin, a hackathon digital twin for emergency-department operations.

This branch combines:

- Harjot's live twin, persistence, direct event processing, WebSocket broadcasting, scenario orchestration, and Gemini narration.
- Ujjwal's acuity-priority simulation and nurse/doctor/bed resource constraints, adapted to the existing Spring simulation contract.
- Daksh's arrival forecasting, surge risk, predictive bottlenecks, recommendation scoring, AI endpoints, grounding, and tests.

This is an operational decision-support prototype. It is not a clinically validated medical device, and its risk and confidence values are not clinical probabilities.

## Run

```bash
cp .env.example .env
docker compose up --build
```

PostgreSQL, Redis, and the backend start together. `GEMINI_API_KEY` is optional: calculated forecasts, bottlenecks, simulations, and recommendation scores work without it; scenario narration uses a deterministic fallback when Gemini is unavailable.

Run the backend directly after starting PostgreSQL and Redis:

```bash
mvn spring-boot:run
```

The default PostgreSQL credentials are `flowtwin` / `flowtwin`, matching `docker-compose.yml`. All settings can still be overridden through environment variables.

## Connected flow

```text
DemoEventSimulator
  -> EventProcessingService
  -> PatientEventRepository + TwinStateService
  -> TwinState
     -> ArrivalForecastService
     -> BottleneckPredictionService
     -> GET /api/ai/insights

TwinState + scenario changes
  -> SimulationEngine
  -> baseline/scenario Metrics
  -> RecommendationEngine
  -> grounded GeminiPromptBuilder
  -> NarrationService (Gemini or fallback)
  -> ScenarioResponse
  -> persistence + WebSocket broadcast
```

The LLM explains supplied calculations. It does not calculate forecasts, bottlenecks, simulation metrics, or recommendation scores.

## REST and WebSocket API

| Method | Path | Output |
|---|---|---|
| GET | `/api/twin/state` | Current `TwinState` |
| GET | `/api/twin/metrics` | Current `TwinState` compatibility endpoint |
| GET | `/api/ai/insights` | State, arrival forecast, surge risk, and bottleneck |
| GET | `/api/ai/forecast` | Forecast only |
| GET | `/api/ai/summary` | Deterministic operational summary |
| POST | `/api/scenarios` | Simulation result, Gemini/fallback narration, and recommendation score |
| GET | `/api/scenarios/{id}` | Persisted scenario summary |
| WS | `/ws/twin` | STOMP endpoint; `/topic/twin` and `/topic/twin/insight` |

Example scenario:

```bash
curl -X POST http://localhost:8080/api/scenarios \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Extra triage nurse",
    "changes": [{"type":"STAFF","role":"NURSE","delta":1}],
    "horizonHours": 4
  }'
```

`ScenarioResponse` keeps Harjot's structured `ChatResponse` and adds Daksh's `recommendation` field:

```json
{
  "id": 1,
  "result": {
    "name": "Extra triage nurse",
    "baseline": {
      "avgTriageWaitMin": 12.4,
      "avgBedWaitMin": 35.1,
      "p90WaitMin": 68.2,
      "peakTriageQueue": 9,
      "bedUtilizationPct": 91.0,
      "totalPatients": 74,
      "completedPatients": 41,
      "avgLengthOfStayMin": 112.5,
      "peakTreatmentQueue": 18
    },
    "scenario": {},
    "delta": {},
    "bottlenecks": []
  },
  "narration": {
    "summary": "...",
    "recommendations": [],
    "tradeoffs": [],
    "source": "gemini"
  },
  "recommendation": {
    "score": 34.6,
    "impact": "HIGH",
    "primaryReason": "...",
    "weightedContributions": {},
    "limitations": []
  }
}
```

The abbreviated empty `{}` objects above indicate the same `Metrics` shape as `baseline`; they are documentation shorthand, not literal runtime output.

## Integrated simulation

The simulation keeps the established `SimulationEngine.run(SimConfig) -> SimResult` API and is deterministic for equal inputs and seeds. It now models:

- Acuity-priority triage and treatment queues.
- Nurses as triage capacity.
- A doctor and a bed as jointly required treatment capacity.
- Acuity-adjusted triage and treatment durations.
- Existing `TwinState` patients, triage queue, and occupied beds at simulation start.
- Peak triage and treatment queues, completed throughput, average completed length of stay, P90 queue wait, and bed utilization.
- Independent per-run state so concurrent scenario calls do not interfere.

The existing metric name `avgBedWaitMin` is retained for API compatibility; it now means the time waiting until both a doctor and bed are available. `TimelinePoint` retains its old three-argument Java constructor and adds `treatmentQueue` to JSON. `SimConfig` and `SimResult` also retain compatibility constructors.

The model does not yet implement scheduled `from`/`to` scenario windows. Service-time distributions and acuity proportions are prototype assumptions that should be calibrated from real operational data.

## Predictive AI

`ArrivalForecastService` queries only `PATIENT_ARRIVED` events from four non-overlapping hourly windows. With sufficient coverage, it blends:

- EWMA with alpha 0.5.
- The recent two-hour mean.
- `TwinState.observedArrivalRatePerHour`.
- A capped and damped recent trend.

Sparse or unavailable history falls back to the observed rate without inventing arrivals. The response identifies its source as `HISTORY_BLEND`, `OBSERVED_RATE`, or `HISTORY_UNAVAILABLE`.

Surge risk is a deterministic weighted score in `[0,1]`:

```text
0.30 arrival growth
+ 0.25 triage queue pressure
+ 0.25 bed occupancy
+ 0.20 forecast triage demand
```

`BottleneckPredictionService` compares explainable pressure ratios for `TRIAGE`, `BEDS`, `DOCTORS`, and `TREATMENT`. Baseline simulation peak queues and bed utilization strengthen the evidence when available.

## Recommendation ranking

`RecommendationEngine` uses only structured simulation metrics. The signed score is bounded to `[-100,100]`:

| Contribution | Weight |
|---|---:|
| P90 queue wait reduction | 30 |
| Average triage wait reduction | 20 |
| Treatment-resource wait reduction | 15 |
| Peak triage queue reduction | 15 |
| Peak treatment queue reduction | 10 |
| Completed throughput increase | 10 |

Lower bed utilization alone is not treated as a benefit. A scenario that removes required nurse, doctor, or bed capacity while demand exists receives `-100`, preventing incomplete service from appearing beneficial because no waits completed.

## Gemini narration

Harjot's `GeminiService`, `GeminiRequest`, `GeminiResponse`, and `ChatResponse` are the provider-specific boundary. `GeminiPromptBuilder` receives the scenario metrics plus the calculated forecast, bottleneck, and recommendation score.

Redis caches the structured `ChatResponse` using a SHA-256 hash of the exact grounded prompt. A cache hit reports `source: "gemini-cached"`. Missing configuration, HTTP failure, invalid model output, or cache failure preserves deterministic fallback behavior.

No patient identifiers or raw patient-event records are sent to Gemini.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/flowtwin` | Database URL |
| `POSTGRES_USER` | `flowtwin` | Database user |
| `POSTGRES_PASSWORD` | `flowtwin` | Database password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `GEMINI_API_KEY` | empty | Optional Gemini narration key |
| `GEMINI_MODEL` | `gemini-2.5-flash` | Gemini model |
| `GEMINI_BASE_URL` | Google Generative Language v1beta | Gemini API base URL |
| `GEMINI_TIMEOUT_MS` | `20000` | Connect/read timeout |

Never commit `.env`; it is ignored.

## Tests

```bash
mvn test
mvn clean package
```

The suite covers arrival trends and fallbacks, every bottleneck class, recommendation direction and safeguards, Gemini/cache fallback, JPA event boundaries, REST compatibility, scenario persistence/broadcasting, deterministic simulation, current-state seeding, doctor/bed constraints, and concurrent run isolation. H2 is test-only; production remains PostgreSQL and Redis.
