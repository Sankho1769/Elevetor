# Product Requirements Document (PRD)

## Project: Infinity Tower — Autonomous Multi-Car Elevator Supervisory & Kinematics Control System (SCADA)

- **Document Version**: `2.1.0-ENTERPRISE`
- **Product Owner**: Senior Systems Architecture & SCADA Engineering Group
- **Status**: `Approved & Implemented`
- **Target Release**: Production Baseline v1.0.0
- **Classification**: Industrial Supervisory & Kinematics Control System

---

## 1. Executive Summary & Product Vision

### 1.1 Product Vision
To engineer a mission-critical, enterprise-grade Supervisory Control and Data Acquisition (SCADA) simulator and dispatching engine capable of autonomous multi-car elevator kinematics modeling, sub-millisecond call scheduling, and high-visibility physical telemetry for modern skyscraper installations.

### 1.2 Problem Statement
Modern skyscraper installations face acute vertical transit bottlenecks during peak up-peak, down-peak, and inter-floor traffic. Traditional elevator controllers often suffer from:
1. **Inefficient Dispatching**: Rigid single-rule dispatchers (e.g., pure FCFS or collective) creating long waiting queues and passenger frustration.
2. **Lack of Transparent SCADA Telemetry**: Building operators often lack sub-second visibility into cabin velocities, door states, motor traction temperatures, and passenger manifests.
3. **Safety Interlock Testing Challenges**: Testing physical emergency protocols (Fire Recall Phase I/II, Seismic Isolation, Flood Interlocks, Motor Lockups) in a physical skyscraper is costly, disruptive, and dangerous.
4. **Heavyweight Infrastructure Overhead**: Existing simulation tools rely on external relational databases, leading to connection pooling latency, query contention, and complex setup procedures.

### 1.3 Proposed Solution
**Infinity Tower ECS** provides a lightweight, zero-database in-memory reactive simulation platform with:
- Deterministic sub-millisecond dispatch cycles across 10 switchable industrial scheduling algorithms.
- Physical kinematics modeling (jerk-limited acceleration, deceleration curves, optical door dwell timing).
- Real-time 150ms WebSocket streaming with a luxury claymorphic porcelain telemetry UI.
- Simulated building emergency triggers and component-level fault injection conforming to ASME A17.1 and EN 81-20/50 safety standards.

---

## 2. Target User Personas & Stakeholders

| Persona | Role | Key Needs & Pain Points | Primary System Interactions |
|---|---|---|---|
| **Marcus Vance** | Chief Facility SCADA Operator | Needs real-time, zero-lag visibility over all 4–16 shafts, instant emergency override controls, and clear fault diagnostics. | Topbar command deck, live 3D telemetry shaft, safety alarm panels, acoustic chimes. |
| **Dr. Elena Rostova** | Elevator Traffic & Algorithm Scientist | Benchmarks dispatching strategies (SCAN vs LOOK vs Destination Dispatch) under variable Poisson arrival rates. | Algorithm hot-swapping tab, traffic generator configurations, passenger wait-time analytics. |
| **Rajesh Patel** | Fire & Safety Compliance Auditor | Validates ASME A17.1 / EN 81 safety interlocks, fire recall floor homing, and fault recovery. | Safety interlocks deck, component fault injector, audit trail ring-buffer query engine. |
| **DevOps / Integration Engineer** | Platform Engineer | Integrates elevator telemetry into building management systems (BMS) via REST/WebSocket APIs. | REST endpoints (`/api/*`), WebSocket stream (`/ws/simulation`), Prometheus metrics (`/actuator/metrics`). |

---

## 3. Product Scope & Boundary Definitions

### 3.1 In-Scope Capabilities
- **Fleet Scalability**: Fleets of 1 to 16 elevator cabins servicing from 2 up to 30 vertical landings (including basements).
- **Multi-Algorithm Dispatching**: Real-time hot-swapping between 10 standard algorithms without restarting state.
- **Physical Kinematics Engine**: Real-time velocity profiling, deceleration distance calculations, and door cycling.
- **Zero-Database Concurrency**: Pure in-memory reactive data structures with bounded circular ring buffers.
- **High-Fidelity Telemetry**: 60/120 FPS hardware-accelerated canvas with smooth exponential interpolation.
- **Synthesized Acoustic Telemetry**: Native Web Audio bell chimes and emergency warbles.
- **Comprehensive API & Observability**: Complete REST CRUD, WebSocket STOMP/raw streaming, and Spring Boot Actuator/Micrometer metrics.

### 3.2 Out-of-Scope (Non-Goals)
- Physical hardware CAN bus micro-controller flashing (handled via simulated gateway).
- Video camera CCTV streaming inside elevator cabins.
- Billing and fare collection for commercial passenger transit.

---

## 4. Functional Requirements (User Epics & Acceptance Criteria)

### Epic 1: High-Performance Kinematic & Physics Simulation Engine

#### FR-1.1: Jerk-Limited Cabin Acceleration & Deceleration
- **Requirement**: Each elevator car must model acceleration from rest to cruise velocity ($v_{\max} = 2.5 \text{ m/s}$) at rate $a = 1.0 \text{ m/s}^2$ and compute braking distance:
  $$d_{\text{brake}} = \frac{v^2}{2 \cdot d_{\text{decel}}}$$
- **Acceptance Criteria**: The car must begin decelerating at exactly $d_{\text{brake}}$ before the target floor and level smoothly to 0 velocity with $< 0.01 \text{ m}$ leveling error.

#### FR-1.2: Realistic Optical Door Dwell Cycling
- **Requirement**: Elevator doors must cycle through `CLOSED` $\rightarrow$ `OPENING` $\rightarrow$ `OPEN` (Dwell) $\rightarrow$ `CLOSING` $\rightarrow$ `CLOSED`.
- **Acceptance Criteria**: Dwell time must dynamically expand when multiple passengers board or alight ($t_{\text{dwell}} = t_{\text{load}} + t_{\text{unload}}$) and abort closing immediately upon obstruction sensor activation.

---

### Epic 2: Multi-Algorithm Supervisory Dispatching Engine

#### FR-2.1: On-the-Fly Algorithm Selection
- **Requirement**: The system must provide 10 distinct, switchable dispatching algorithms:
  1. `scan`: Bi-directional elevator sweep terminal-to-terminal.
  2. `look`: Sweep reversing when no calls exist ahead.
  3. `fcfs`: First-come, first-served FIFO queue.
  4. `sstf`: Shortest seek time first (greedy distance minimization).
  5. `collective`: Two-button full collective control.
  6. `nearest`: Multi-car kinematic ETA allocation.
  7. `sector`: Static building zoning (Low/Mid/High-Rise).
  8. `destination`: Destination dispatch grouping riders by floor.
  9. `adaptive`: Queue-depth dynamic strategy switcher.
  10. `ai`: Multi-objective weighted scoring function.
- **Acceptance Criteria**: Algorithm switching must take effect instantaneously via `POST /api/algorithm` without resetting running cars or dropping pending hall calls.

---

### Epic 3: Emergency Interlocks & Fault Injection (Safety Subsystem)

#### FR-3.1: Building-Wide Emergency Modes
- **Requirement**: Provide building-wide toggles for Fire Recall (Phase I), Smoke Detection, E-Stop, Power Failure UPS, Seismic Shock, and Basement Flood.
- **Acceptance Criteria**:
  - `Fire Alarm`: Immediately cancel all calls, command cars to proceed directly to Floor 1 (Lobby), open doors fully, and lock in recall mode.
  - `Flood Alarm`: Clear all basement stops ($f \le 0$) and prevent calls originating from or traveling to basement landings.

#### FR-3.2: Component-Level Fault Injection
- **Requirement**: Allow operators to inject faults into individual cars (`motor`, `brake`, `sensor`, `door`, `comm`, `cable`).
- **Acceptance Criteria**: Faulted cars must immediately disengage from dispatch eligibility; pending calls must be automatically redispatched to healthy fleet vehicles.

---

### Epic 4: Real-Time SCADA Telemetry & Interactive Supervisory Deck

#### FR-4.1: Luxury Claymorphic High-Visibility UI
- **Requirement**: High-visibility porcelain claymorphic dashboard matching enterprise specifications:
  - Floating white clay cards with inset specular lighting and subtle shadows.
  - Overhead machine room mezzanine displaying 3D brushed-chrome hoistway doors (`CAR A`..`CAR D`).
  - 3D sapphire metallic elevator pods with passenger icons, split doors, and amber underglow.
  - Active floor peach highlight bands (`rgba(254, 215, 170, 0.70)`) and cyan `VIP` badges.
- **Acceptance Criteria**: 60 FPS rendering under continuous telemetry streaming with zero layout shifts.

#### FR-4.2: Interactive Click-to-Call & Hotkeys
- **Requirement**: Operator must be able to click any landing on the shaft canvas to dispatch a call to that floor, and use keyboard shortcuts (`Space` Pause/Resume, `R` Reset, `1-8` Speed, `M` Mute).

#### FR-4.3: Synthesized Acoustic Telemetry
- **Requirement**: Web Audio synthesizer generating harmonic bell chimes on floor arrival ($D_5 \rightarrow A_5$) and alert warbles on emergency trips.

---

## 5. Non-Functional Requirements (NFRs)

| ID | Category | Requirement Specification | Measurement Metric |
|---|---|---|---|
| **NFR-1** | **Performance & Latency** | REST API dispatch endpoints must execute with sub-millisecond response time. | $p_{99} < 5.0 \text{ ms}$ under 1,000 req/sec |
| **NFR-2** | **Concurrency & Thread-Safety** | Lock-free command pipeline must field 50+ concurrent operator threads without deadlocks or race conditions. | 1,000 commands enqueued in $< 150 \text{ ms}$ |
| **NFR-3** | **Memory Footprint** | Bounded in-memory architecture must never exceed a constant memory envelope during continuous 24/7 operation. | JVM Heap usage $\le 128 \text{ MB}$; 5,000 log cap |
| **NFR-4** | **Observability** | Expose standard Prometheus/Micrometer metrics for dispatch latency, tick latency, and queue depth. | Available at `/actuator/metrics` |
| **NFR-5** | **Zero Database Dependency** | System must boot into production without external database services (PostgreSQL, MySQL, Redis). | Standalone executable JAR |
| **NFR-6** | **Browser Compatibility** | SCADA UI must function smoothly across Chromium, Firefox, and Safari with zero external npm dependencies. | 60 FPS HTML5 Canvas |

---

## 6. Success Metrics & Key Performance Indicators (KPIs)

1. **Average Passenger Waiting Time ($T_{\text{wait}}$)**: Maintain $< 15.0 \text{ seconds}$ during standard building traffic.
2. **Dispatch Algorithm Throughput**: Evaluate $\ge 500 \text{ calls/second}$ per dispatch engine instance.
3. **Telemetry Streaming Fidelity**: Zero packet drops with consistent 150ms WebSocket broadcasts.
4. **Zero-Defect Safety Interlocks**: 100% pass rate across fire recall, flood isolation, and motor failure tests.
5. **Code Reliability**: 28/28 automated unit, integration, and high-concurrency benchmark tests passing.

---

## 7. Product Release Roadmap

- [x] **Phase 1: Core Physics & Reactive In-Memory Engine** (Java 21, Spring Boot 3.3.4, zero-DB architecture).
- [x] **Phase 2: 10 Industrial Scheduling Algorithms** (SCAN, LOOK, FCFS, SSTF, Collective, Destination Dispatch, etc.).
- [x] **Phase 3: SCADA Supervisory Telemetry Deck** (WebSocket streaming, 150ms updates, REST API).
- [x] **Phase 4: Luxury Claymorphic UI/UX Modernization** (Porcelain surfaces, 3D chrome hoistway doors, sapphire pods).
- [x] **Phase 5: Lock-Free Command Pipeline & Enterprise Observability** (ConcurrentLinkedQueue, Micrometer metrics, Web Audio chimes).
- [ ] **Phase 6 (Future)**: Multi-building cluster federation via gRPC and hardware BACnet/IP gateway adapter.
