# Infinity Tower — Elevator Control System (Backend)

A real Spring Boot + MySQL + WebSocket backend for the Infinity Tower elevator simulator, with the
matching frontend served from the same origin so there is nothing to configure for CORS in normal use.

## What this is

- **Backend**: Java 21, Spring Boot 3.3, Spring Data JPA, MySQL, native WebSocket (no STOMP client needed).
- **Frontend**: `src/main/resources/static/index.html` — same visual design as the standalone version,
  but every action goes over REST and the live simulation state streams in over WebSocket. Spring Boot
  serves it automatically at `http://localhost:8080/`.
- **Scheduler**: all 10 algorithms (FCFS, SCAN, LOOK, SSTF, Collective Control, Nearest Car, Sector
  Based, Destination Dispatch, Adaptive, AI-Inspired Dynamic) ported to Java as separate strategy
  classes, each genuinely distinct (see `src/main/java/.../simulation/*Scheduler.java`).
- **Persistence**: building configuration, simulation logs, and exported reports are stored in MySQL.
  Live per-tick elevator/passenger state stays in memory (as it should — that's ephemeral simulation
  state, not something you want hammering a database every 100ms).

## Prerequisites

- JDK 21 (`java -version` should show 21.x)
- Maven 3.9+ (or use your IDE's built-in Maven)
- MySQL 8.x running locally (or reachable) — the app can auto-create the database if the user has
  privileges, it will not create the MySQL server itself

## 1. Create the database user/credentials (or reuse existing ones)

The default config in `src/main/resources/application.yml` assumes:

```
url: jdbc:mysql://localhost:3306/infinity_tower?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
username: root
password: root
```

Edit `src/main/resources/application.yml` if your MySQL credentials differ. The schema
(`src/main/resources/schema.sql`) runs automatically on every startup (`CREATE TABLE IF NOT EXISTS`,
safe to re-run).

## 2. Build

```bash
cd infinity-tower-backend
mvn clean package
```

This downloads Spring Boot/MySQL driver dependencies from Maven Central — that step could not be
verified in the environment this was written in (no Maven Central access there), so if you hit a
dependency resolution or compile error on your machine, send me the exact error and I'll fix it fast.

## 3. Run

```bash
mvn spring-boot:run
```

or after packaging:

```bash
java -jar target/elevator-control-system-1.0.0.jar
```

Then open **http://localhost:8080** — that's the frontend, already wired to the backend.

## API surface (all under `/api`)

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/config` | GET / PUT | Read/update building configuration (persisted) |
| `/api/simulation/start` `/pause` `/resume` `/reset` | POST | Lifecycle control |
| `/api/simulation/speed` | POST `{multiplier}` | 1x/2x/4x/8x |
| `/api/simulation/snapshot` | GET | One-shot full state (WebSocket is the live feed) |
| `/api/requests/spawn` | POST | Manual passenger/request generation |
| `/api/requests/batch?count=10` | POST | Batch random generation |
| `/api/requests/autogen` | POST `{mode, ratePerMinute}` | Traffic pattern (peak, rush, office, etc.) |
| `/api/algorithm` | GET | List all 10 algorithms with descriptions |
| `/api/algorithm` | POST `{algorithm}` | Switch scheduler live |
| `/api/safety` | GET | Current safety state |
| `/api/safety/toggle` | POST `{key}` | fire / smoke / estop / power / earthquake / flood |
| `/api/safety/clear-all` | POST | Reset every alarm at once |
| `/api/safety/fault` | POST `{elevatorId, action, faultType}` | action: inject / clear / maintenance / inspection / independent |
| `/api/analytics` | GET | Live analytics snapshot |
| `/api/reports?type=...&format=json\|csv` | GET | Generates + persists + downloads a report |
| `/api/logs?category=&search=&limit=` | GET | Searchable/filterable persisted log history |

**WebSocket**: `ws://localhost:8080/ws/simulation` — pushes a full JSON state snapshot roughly every
150ms while at least one client is connected. No subscribe/handshake needed beyond opening the socket.

## Running the tests

```bash
mvn test
```

`SchedulerTest` verifies the 10 algorithms make genuinely different dispatch decisions (not just
different names on the same logic). `SimulationEngineTest` covers passenger flow, fire recall
clearing, flood-mode basement locking, hard vs. soft faults, and the stuck-hall-call recovery sweep.

## Known limitations / honest notes

- Live elevator/passenger state is in-memory only — restarting the backend resets the simulation
  (building config and historical logs/reports survive in MySQL, the live simulation itself does not).
  Adding live-state persistence/recovery is a reasonable next step if you need it.
- CORS is wide open (`allowedOrigins("*")`) for ease of local development. Tighten this before
  exposing the API beyond your own machine.
- I could not run `mvn compile` against real Spring Boot/MySQL dependencies while building this (no
  Maven Central access in my environment) — I syntax-checked every file with a bare JDK and traced the
  logic carefully, but a first real `mvn clean package` on your machine is the actual compile test.
  Send me anything that doesn't build and I'll fix it immediately.
