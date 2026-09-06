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

- **Not compiled here** — written to standard conventions but build it yourself (`mvn package`) before relying on it.
- Time-windowed scenario changes (`from`/`to`) are parsed but applied for the whole horizon — see `ScenarioOrchestrator.applyChanges`.
- Service-time and arrival assumptions are constants/rough estimates — replace with values learned from history.
- Only an OpenAI-compatible provider is implemented; add Gemini/Claude providers behind `NarrationProvider`.
- Redis stores headline metrics only; full snapshot serialization is a TODO.
- Spring Boot 4 notes: Jackson 3 is the default; JUnit 4 is dropped (use JUnit 5); check security defaults before exposing endpoints.
