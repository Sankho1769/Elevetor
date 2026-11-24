# Software Requirements Specification (SRS)

## Project: Infinity Tower — Autonomous Multi-Car Elevator Supervisory & Kinematics Control System (SCADA)

- **Standard**: Conforming to IEEE Std 830-1998 / ISO/IEC/IEEE 29148:2018
- **Document Version**: `2.1.0-ENTERPRISE`
- **Status**: `Approved Baseline`
- **System Classification**: Mission-Critical Industrial SCADA & Kinematics Simulation

---

## 1. Introduction

### 1.1 Purpose
This Software Requirements Specification (SRS) describes the complete functional, non-functional, kinematic, and behavioral requirements for the **Infinity Tower Elevator Control & Simulation System (ECS)**. It serves as the definitive engineering contract between systems architects, software engineers, facility SCADA operators, and safety certification auditors.

### 1.2 Scope
The Infinity Tower ECS software package encompasses:
- An in-memory, zero-database, high-concurrency simulation core modeling physical elevator mechanics, kinematics, and traffic patterns.
- An extensible supervisory dispatching engine housing 10 hot-swappable industrial scheduling algorithms.
- A lock-free command pipeline and wait-free state snapshot publishing engine.
- A low-latency binary/JSON WebSocket streaming pipeline broadcasting real-time telemetry every 150 milliseconds.
- A web-based, hardware-accelerated 60/120 FPS luxury claymorphic SCADA interface featuring 3D hoistway doors, animated capsules, synthesized Web Audio acoustic feedback, and click-to-call dispatching.

### 1.3 Definitions, Acronyms, and Abbreviations

| Term | Definition |
|---|---|
| **SCADA** | Supervisory Control and Data Acquisition |
| **ECS** | Elevator Control System |
| **ETA** | Estimated Time of Arrival |
| **FCFS** | First-Come, First-Served Dispatching |
| **SSTF** | Shortest Seek Time First Dispatching |
| **SCAN** | Bi-directional sweeping elevator algorithm (Elevator Algorithm) |
| **LOOK** | Directional sweeping elevator algorithm that reverses when no calls remain ahead |
| **ASME A17.1** | Safety Code for Elevators and Escalators (American National Standard) |
| **EN 81-20/50** | European Standard for Elevator Construction and Safety Requirements |
| **Ring Buffer** | Fixed-size circular memory buffer that overwrites oldest entries when full |
| **Lerp** | Linear/Exponential Interpolation between discrete states |
| **Web Audio API** | High-level JavaScript API for processing and synthesizing audio in web applications |

### 1.4 References
1. IEEE Std 830-1998: *IEEE Recommended Practice for Software Requirements Specifications*.
2. ISO/IEC/IEEE 29148:2018: *Systems and software engineering — Life cycle processes — Requirements engineering*.
3. ASME A17.1 / CSA B44: *Safety Code for Elevators and Escalators*.
4. EN 81-20:2020: *Safety rules for the construction and installation of lifts — Lifts for the transport of persons and goods*.

---

## 2. Overall System Description

### 2.1 Product Perspective
Infinity Tower ECS is an autonomous, standalone supervisory control platform. It can function as an isolated test-bed for algorithmic research, a digital twin simulator for facility engineers, or an operational supervisory layer interfaced with physical building management systems via standard REST and WebSocket protocols.

```
+-------------------------------------------------------------------------+
|                         SCADA OPERATOR INTERFACE                        |
|  (60 FPS HTML5 Canvas, Luxury Claymorphic Deck, Web Audio Synthesizer)  |
+------------------------------------+------------------------------------+
                                     |  HTTP REST & WebSocket (150ms)
+------------------------------------v------------------------------------+
|                       API & COMMUNICATION GATEWAY                       |
|   (Spring MVC REST Controllers, SimulationSocketHandler, Actuator)     |
+------------------------------------+------------------------------------+
                                     |  O(1) Lock-Free Command Ingress
+------------------------------------v------------------------------------+
|                         SIMULATION SERVICE                              |
|       (100ms Scheduled Tick Task, 150ms Broadcast Task, Metrics)        |
+------------------------------------+------------------------------------+
                                     |  Batch Draining & Tick Physics
+------------------------------------v------------------------------------+
|                      SIMULATION ENGINE (CORE)                           |
|  - ConcurrentLinkedQueue<SimulationCommand>                             |
|  - Volatile Wait-Free Snapshot Publishing (cachedSnapshot)             |
|  - BuildingModel, ElevatorFleet (1-16 Cars), SafetyStateModel          |
|  - SchedulerRegistry (10 Industrial Algorithms)                        |
|  - Bounded 5,000 Event Circular Ring Buffer                             |
+-------------------------------------------------------------------------+
```

### 2.2 User Classes and Operating Profiles
- **SCADA Supervisory Operators**: Continuous monitoring, emergency alarms, fault clearance.
- **Traffic Analysis Engineers**: Execution of batch passenger injections and algorithmic benchmarking.
- **Compliance & Safety Inspectors**: Verification of interlock trip times and recall homing sequences.
- **Automated Client Systems**: External BMS systems consuming JSON telemetry feeds.

### 2.3 Design and Implementation Constraints
- **Language & Runtime**: Java 21 LTS (utilizing records, sealed interfaces, and pattern matching).
- **Framework**: Spring Boot 3.3.4 (Standalone, embedded Tomcat, zero external database).
- **Memory Footprint**: Strict maximum heap constraint $\le 128 \text{ MB}$; event logs bounded to 5,000 entries.
- **Frontend Architecture**: Pure HTML5/Canvas/CSS3/JavaScript with zero node_modules or external bundler dependency.

---

## 3. Specific System Requirements

### 3.1 Kinematics & Physics Engine Requirements

#### SRS-KIN-001: Kinematic Velocity Profiling
- The engine shall compute cabin displacement $\Delta y$ per discrete simulation step $\Delta t$:
  $$\Delta y = v \cdot \Delta t$$
- Cabin speed $v$ shall be bounded by $0 \le v \le v_{\max}$, where $v_{\max} = 2.5 \text{ m/s}$ (normal service) or $v_{\max} = 0.5 \text{ m/s}$ (inspection mode).

#### SRS-KIN-002: Braking Distance Calculation
- The engine shall calculate stopping distance continuously:
  $$d_{\text{stop}} = \frac{v^2}{2 \cdot a_{\text{decel}}}$$
- When distance to destination floor $|f_{\text{target}} - y| \le d_{\text{stop}}$, the elevator shall transition from cruise speed to constant deceleration $a_{\text{decel}} = 1.0 \text{ m/s}^2$.

#### SRS-KIN-003: Door Timing & Optical Sensor Interlocks
- Door opening duration shall be governed by `building.doorOpenTime` (default: $2.0 \text{ s}$).
- Door closing duration shall be governed by `building.doorCloseTime` (default: $2.0 \text{ s}$).
- Optical passenger boarding dwell time shall scale dynamically:
  $$t_{\text{dwell}} = t_{\text{load}} \cdot N_{\text{boarding}} + t_{\text{unload}} \cdot N_{\text{alighting}}$$
- If a door fault (`door`) is active, doors shall abort closure, transition to `OPENING`, and dwell open.

---

### 3.2 Lock-Free Command Pipeline & Concurrency Requirements

#### SRS-CON-001: Asynchronous Command Submission
- External HTTP requests shall submit commands through a lock-free `ConcurrentLinkedQueue`:
  ```java
  public void submitCommand(SimulationCommand command);
  ```
- Command submission shall execute in $\mathcal{O}(1)$ without acquiring monitor locks on `SimulationEngine`.

#### SRS-CON-002: Batch Command Draining
- At the start of each physics tick ($100 \text{ ms}$ interval), the simulation thread shall drain all pending commands via `commandQueue.poll()` in batch before executing kinematic calculations.

#### SRS-CON-003: Wait-Free Telemetry Snapshot Reads
- The simulation thread shall publish an immutable `volatile Dto.SnapshotDto cachedSnapshot` at the completion of every tick.
- Read operations (`getSnapshot()`) shall return `cachedSnapshot` directly without synchronizing or acquiring locks, achieving wait-free reads for all concurrent HTTP sessions.

---

### 3.3 Dispatching Scheduling Strategies

The system shall implement the following 10 scheduling algorithms via the `SchedulingAlgorithm` interface:

| Requirement ID | Algorithm | Mathematical Dispatch Decision Logic | Complexity |
|---|---|---|:---:|
| **SRS-ALG-001** | **SCAN** | Car traverses entire shaft from bottom floor to top floor, serving all calls in current direction, then reverses at terminal landing. | $\mathcal{O}(N \log N)$ |
| **SRS-ALG-002** | **LOOK** | Car sweeps in current direction and reverses immediately when $\forall c \in \text{Calls}, \text{sign}(c - y) \ne \text{direction}$. | $\mathcal{O}(N \log N)$ |
| **SRS-ALG-003** | **FCFS** | Allocates vehicle strictly in timestamp order of call generation: $\min(t_{\text{call}})$. | $\mathcal{O}(N)$ |
| **SRS-ALG-004** | **SSTF** | Allocates car minimizing instantaneous floor distance: $\arg\min_e |y_e - f_{\text{call}}|$. | $\mathcal{O}(K \cdot N)$ |
| **SRS-ALG-005** | **Collective** | Groups coinciding hall calls along the travel vector; handles opposite direction calls on return leg. | $\mathcal{O}(N)$ |
| **SRS-ALG-006** | **Nearest Car** | Evaluates kinetic ETA accounting for velocity, intermediate stops, and direction matching. | $\mathcal{O}(K)$ |
| **SRS-ALG-007** | **Sector** | Matches call floor $f$ to designated car sector: $f \in [\text{sectorMin}_e, \text{sectorMax}_e]$. | $\mathcal{O}(1)$ |
| **SRS-ALG-008** | **Destination Dispatch** | Groups passengers by target floor prior to boarding, minimizing total unique stops: $\text{score} = \text{cost} - 1.5 \cdot N_{\text{shared\_dest}}$. | $\mathcal{O}(K \cdot M)$ |
| **SRS-ALG-009** | **Adaptive** | Dynamically shifts strategy based on pending queue load: Collective under low load, Sector under medium load, Express Destination under peak load. | $\mathcal{O}(K \cdot N)$ |
| **SRS-ALG-010** | **AI Dynamic** | Multi-factor weighted utility optimization: $S = -2.2 \cdot \Delta y + 6 \cdot I_{\text{dir}} - 1.8 \cdot |Q| - 5.0 \cdot \frac{W}{W_{\max}} + 2.5 \cdot P_{\text{vip}} + 3.0 \cdot I_{\text{idle}}$. | $\mathcal{O}(K \cdot N)$ |

---

### 3.4 Building Safety Systems & Interlock Requirements

#### SRS-SAF-001: Phase I Emergency Fire Recall (ASME A17.1)
- Upon activation of `fire` alarm:
  1. All active hall and cabin calls shall be cancelled immediately.
  2. All cars shall reverse or proceed non-stop to the designated egress landing (Floor 1 / Lobby).
  3. Upon arrival at the lobby, cars shall open doors fully and enter shutdown lock.
  4. Hall call dispatching shall be blocked until `fire` is reset.

#### SRS-SAF-002: Basement Flood Isolation Interlock
- Upon activation of `flood` alarm:
  1. All basement floor calls ($f \le 0$) shall be cancelled from all car target plans.
  2. The dispatching engine shall reject any new requests originating from or destined to basements ($f < 0$).
  3. Cars positioned in basements shall evacuate to Floor 1 immediately.

#### SRS-SAF-003: Component Fault Injection & Failsafe Tripping
- Supported fault types:
  - `motor`: Hoist traction motor failure $\rightarrow$ immediate mechanical brake lock, speed set to 0.
  - `brake`: Seized mechanical brake $\rightarrow$ deceleration trip.
  - `sensor`: Optical shaft encoder desync $\rightarrow$ triggers safe homing routine.
  - `door`: Optical curtain obstruction $\rightarrow$ forces door to remain open.
  - `comm`: CAN bus supervisory telemetry dropout $\rightarrow$ car excluded from dispatch eligibility.
  - `cable`: Traction cable tension anomaly $\rightarrow$ speed clamped to inspection velocity.

---

### 3.5 Real-Time Telemetry & Communication Interfaces

#### SRS-COM-001: Low-Latency WebSocket Telemetry Pipeline
- The system shall stream telemetry packets to connected clients every $150 \text{ ms}$ over `/ws/simulation`.
- Broadcast payloads shall include:
  - Simulation clock: `simDay`, `simSeconds`, `running`.
  - Fleet state array: `id`, `label`, `currentFloor`, `direction`, `speed`, `doorState`, `doorProgress`, `passengerCount`, `weight`, `fault`.
  - Analytics aggregate: `waitingPassengers`, `completed`, `avgWaitTime`, `energy`, `movingCars`.
- Disconnected sessions shall be cleanly purged from `CopyOnWriteArraySet` with debug logging.

#### SRS-COM-002: RESTful API Contract
- The system shall expose REST interfaces for configuration (`/api/config`), lifecycle (`/api/simulation/*`), passenger requests (`/api/requests/*`), algorithm management (`/api/algorithm`), safety (`/api/safety/*`), analytics (`/api/analytics`), and reports (`/api/reports`).

---

### 3.6 Frontend User Interface & Audio Telemetry Requirements

#### SRS-UI-001: Hardware-Accelerated 60/120 FPS Rendering
- The supervisory deck shall render the shaft telemetry via HTML5 2D Canvas driven by `requestAnimationFrame`.
- Car positions and door progress shall be interpolated using continuous exponential decay:
  $$x_{\text{render}} = x_{\text{render}} + (x_{\text{target}} - x_{\text{render}}) \cdot (1 - e^{-18 \cdot \Delta t})$$
- Passenger data shall be pre-bucketed into hash maps $\mathcal{O}(1)$ on packet arrival.

#### SRS-UI-002: Interactive Click-to-Call Floor Dispatching
- Clicking anywhere within a floor row on the shaft canvas shall compute target floor landing $f = \lfloor (y_{\text{click}} - y_{\text{top}}) / h_{\text{row}} \rfloor$ and submit an automatic passenger call via `POST /api/requests/spawn`.

#### SRS-UI-003: Synthesized Web Audio Acoustic Telemetry
- The browser interface shall synthesize acoustic feedback using the native Web Audio API:
  - **Arrival Bell**: Harmonic sine/triangle chime ($D_5 \rightarrow A_5$, $587.33 \text{ Hz} \rightarrow 880 \text{ Hz}$) triggered on door opening.
  - **Emergency Alert**: Sawtooth frequency sweep ($440 \text{ Hz} \rightarrow 220 \text{ Hz}$) triggered on alarm activation.
- Telemetry chimes shall be toggleable via topbar button or `M` hotkey.

---

## 4. Non-Functional Requirements (NFRs)

### 4.1 Performance & Scalability
- **NFR-PERF-1**: Single-tick physics calculation across 16 cars shall complete in $< 1.5 \text{ ms}$.
- **NFR-PERF-2**: Lock-free command queue shall sustain $\ge 5,000 \text{ commands/second}$ without thread stalls.
- **NFR-PERF-3**: In-memory event audit trail ring buffer shall cap storage at 5,000 entries with $\mathcal{O}(1)$ push and eviction.

### 4.2 Safety & Integrity
- **NFR-SAFE-1**: Failsafe default states: if an elevator experiences an unhandled fault, its velocity must immediately clamp to 0 and all passengers must be preserved in state.
- **NFR-SAFE-2**: Concurrency integrity: reading threads shall never observe partially updated (torn) elevator state vectors.

### 4.3 Reliability & Availability
- **NFR-REL-1**: Standalone zero-database architecture: system shall boot and reach full operational readiness in $< 3.0 \text{ seconds}$.
- **NFR-REL-2**: Continuous operation: zero memory leaks over 24/7 execution cycle.

---

## 5. Requirements Traceability Matrix (RTM)

| Requirement ID | Requirement Description | Verification Method | Associated Test Class / Test Method |
|---|---|---|---|
| **SRS-KIN-001** | Kinematic velocity & deceleration profiling | Automated Test | `SimulationEngineTest.elevatorEventuallyReachesRequestedFloor` |
| **SRS-KIN-003** | Door dwell timing & passenger boarding | Automated Test | `SimulationEngineTest.elevatorEventuallyReachesRequestedFloor` |
| **SRS-CON-001** | Lock-free command pipeline submission | Concurrency Test | `LockFreeCommandPipelineConcurrencyTest.testConcurrentCommandSubmissionAndDraining` |
| **SRS-CON-003** | Wait-free snapshot publishing | Integration Test | `SimulationRestApiIntegrationTest.testSimulationLifecycle` |
| **SRS-ALG-001..010** | 10 Industrial scheduling algorithms | Unit Test | `SchedulerTest.allSchedulersProduceValidDecisions` |
| **SRS-SAF-001** | Phase I Emergency fire recall | Integration Test | `SimulationEngineTest.fireAlarmForcesRecallAndClearingReleasesElevators` |
| **SRS-SAF-002** | Basement flood isolation protocol | Unit Test | `SimulationEngineTest.floodModeBlocksBasementRequests` |
| **SRS-SAF-003** | Component fault injection & recovery | Unit Test | `SimulationEngineTest.hardFaultStopsElevatorButCommFaultOnlyBlocksNewDispatch` |
| **SRS-COM-001** | WebSocket telemetry streaming | Integration Test | `SimulationWebSocketIntegrationTest.testWebSocketTelemetryFeed` |
| **SRS-COM-002** | Complete REST API contract | Integration Test | `SimulationRestApiIntegrationTest.*` (9 test cases) |
| **SRS-UI-001** | 60 FPS Canvas rendering & lerp | Code Inspection | `src/main/resources/static/index.html` |
| **SRS-UI-002** | Click-to-call canvas dispatching | Code Inspection | `src/main/resources/static/index.html` (Canvas click listener) |
| **SRS-UI-003** | Web Audio synthesized acoustic telemetry | Code Inspection | `src/main/resources/static/index.html` (`audio.playChime()`) |
