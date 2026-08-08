# Ambulance Call Center and Dispatch System

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-green.svg)
![React](https://img.shields.io/badge/React-19-blue.svg)

## Description

The Ambulance Call Center and Dispatch System is a Java 21 emergency medical dispatch application designed to demonstrate two primary software-engineering goals: **Spring Boot MVC architecture** and the purposeful use of **data structures and algorithms** inside a realistic dispatch domain.

The application separates the user interface, HTTP/web layer, application coordination, domain behavior, and in-memory state. React acts as the dispatcher-facing view, Spring MVC `DispatchController` receives HTTP requests, `AmbulanceDispatchFacade` exposes coarse-grained application use cases, and the domain layer contains the objects and invariants that define emergency-call and ambulance behavior. `AmbulanceCallCenter` owns the authoritative in-memory state and coordinates the core collections used by the system.

The primary design pattern is **Facade**. `AmbulanceDispatchFacade` provides the web layer with a small, stable set of dispatch operations such as recommending the next ambulance, approving a recommendation, overriding a recommendation, and acknowledging a dispatch. The Facade prevents the Spring MVC controller from directly coordinating the priority queue, ambulance registry, recommendation service, route/travel estimation, dispatch records, and domain invariants.

The primary data structure is Java's `PriorityQueue<EmergencyCall>`, which is heap-backed. `EmergencyCallComparator` orders waiting calls first by medical `Priority` and then by `arrivalSequence`. This allows a later CRITICAL call to move ahead of an earlier lower-priority call while preserving first-come, first-served behavior for equal-priority calls. `arrivalSequence` also provides deterministic ordering when two calls have the same priority and timestamp.

Additional collections support the rest of the application state: `HashMap` provides fast lookup of ambulances, recommendations, and active dispatches; `HashSet` tracks available ambulance identifiers; and `ArrayList` stores dispatch history. These structures were selected according to the operations the application performs most frequently rather than using one collection type for every responsibility.

The dispatch workflow is human supervised. The CAD portion of the system identifies eligible ambulances and creates a `DispatchRecommendation`, but the Emergency Dispatcher reviews the recommendation and either approves it or selects another appropriate ambulance. The system revalidates the selected ambulance before committing a `DispatchRecord` so that a stale recommendation cannot create an invalid or duplicate assignment.

The project also demonstrates a complete OOAD trace from requirements to implementation. It includes noun analysis, a domain model, use-case diagrams, detailed use-case scenarios, robustness analysis, sequence diagrams, a UML class diagram, BDD scenarios, TDD method traceability, and automated tests. Together, these artifacts explain not only **what** the application does, but **why the classes, data structures, MVC responsibilities, and design-pattern boundaries are arranged the way they are**.


## Problem

An emergency dispatch center needs a consistent way to determine which EmergencyCall should be handled first and which Ambulance should respond.

A simple first-come, first-served queue is not sufficient because a later CRITICAL EmergencyCall may need to be handled before an earlier LOW-priority EmergencyCall. At the same time, equal-priority calls still need to remain first come, first served. Two calls may also have the same Priority and the same timestamp, so the system needs another deterministic ordering value.

The system must also distinguish between an Ambulance that is active/on duty and an Ambulance that is actually available. An Ambulance may be staffed and active but unavailable because it is already DISPATCHED, EN_ROUTE, ON_SCENE, TRANSPORTING, AT_HOSPITAL, REFUELING, in MAINTENANCE, or otherwise unable to accept another call.

After selecting the next EmergencyCall, the system must determine which Ambulances are available and appropriate for that emergency. Clinical capability, jurisdiction, mutual-aid authorization, current Location, and travel time may all affect the recommendation. The Emergency Dispatcher must still be able to review and override the recommendation before the assignment becomes a confirmed Dispatch.

The proposed system solves these problems by combining a stable PriorityQueue for waiting EmergencyCalls with a human-supervised CAD recommendation and dispatch workflow.


## Project Objectives

This project is intentionally centered on two technical themes.

### 1. Demonstrate Spring Boot MVC

The application demonstrates MVC by separating web concerns from application and domain concerns. The React interface presents dispatcher information and sends user actions to the backend. Spring MVC controllers translate HTTP requests into application calls. The Facade coordinates use cases. Domain objects enforce business rules and own state that belongs to the dispatch problem itself.

The goal is not simply to use `@RestController`. The goal is to show that the controller remains thin because HTTP handling and domain behavior are different responsibilities.

### 2. Demonstrate Data Structures and Algorithm Analysis

The application uses data structures because the dispatch problem requires different access and ordering behaviors:

- A `PriorityQueue<EmergencyCall>` maintains priority-based waiting-call order.
- A `HashMap<Integer, Ambulance>` provides direct ambulance lookup by identifier.
- A `HashSet<Integer>` maintains fast membership checks for available ambulance identifiers.
- A `HashMap<Integer, DispatchRecord>` provides direct lookup of active dispatches by ambulance.
- A `HashMap<Long, DispatchRecommendation>` provides direct lookup of pending recommendations.
- An `ArrayList<DispatchRecord>` maintains ordered dispatch history.

The project documents the expected Big-O cost of the most important operations and explains why a heap-backed priority queue is better suited to the waiting-call problem than a normal FIFO queue or repeatedly sorting a list.

### 3. Demonstrate Object-Oriented Design and Refactoring

The system uses GRASP and SOLID principles to keep responsibilities understandable. Refactoring decisions focused on reducing controller coupling, improving cohesion, extracting queue-ordering policy, separating recommendations from committed dispatch records, protecting domain invariants, and isolating external routing behavior behind provider interfaces.

### 4. Demonstrate Design Traceability

The design artifacts show a progression from problem-domain understanding to implementation:

```text
Requirements
    ↓
Noun Analysis
    ↓
Domain Model
    ↓
Use-Case Diagram
    ↓
Use-Case Scenario
    ↓
Robustness Diagram
    ↓
Sequence Diagram
    ↓
Class Diagram
    ↓
Spring Boot MVC + Facade + Data Structures
    ↓
BDD / TDD / API / Architecture Tests
```


## Table of Contents

- [Ambulance Call Center and Dispatch System](#ambulance-call-center-and-dispatch-system)
  - [Description](#description)
  - [Problem](#problem)
  - [Project Objectives](#project-objectives)
    - [1. Demonstrate Spring Boot MVC](#1-demonstrate-spring-boot-mvc)
    - [2. Demonstrate Data Structures and Algorithm Analysis](#2-demonstrate-data-structures-and-algorithm-analysis)
    - [3. Demonstrate Object-Oriented Design and Refactoring](#3-demonstrate-object-oriented-design-and-refactoring)
    - [4. Demonstrate Design Traceability](#4-demonstrate-design-traceability)
  - [Table of Contents](#table-of-contents)
  - [Design Process](#design-process)
  - [Assumptions and Open Questions](#assumptions-and-open-questions)
  - [Design Decision Log](#design-decision-log)
  - [UML and OOAD Artifact Analysis](#uml-and-ooad-artifact-analysis)
    - [Domain Model](#domain-model)
    - [Use-Case Diagram](#use-case-diagram)
    - [Use-Case Scenario](#use-case-scenario)
    - [Robustness Diagram](#robustness-diagram)
    - [Sequence Diagram](#sequence-diagram)
    - [Class Diagram](#class-diagram)
    - [How the UML Artifacts Connect](#how-the-uml-artifacts-connect)
  - [Noun Analysis](#noun-analysis)
  - [Domain Modeling](#domain-modeling)
  - [Use Cases](#use-cases)
    - [Emergency Dispatcher](#emergency-dispatcher)
    - [Dispatch Ambulance](#dispatch-ambulance)
    - [Ambulance Crew](#ambulance-crew)
    - [Fleet Supervisor](#fleet-supervisor)
    - [Administrator](#administrator)
  - [UML Class Diagram](#uml-class-diagram)
    - [Classes](#classes)
  - [Application Flow](#application-flow)
    - [BDD Scenarios](#bdd-scenarios)
  - [TDD Traceability to Methods](#tdd-traceability-to-methods)
  - [Class / Method                                      TDD Test](#class--method--------------------------------------tdd-test)
    - [Traceability Summary](#traceability-summary)
  - [Spring Boot MVC Architecture Analysis](#spring-boot-mvc-architecture-analysis)
    - [Why This Is MVC](#why-this-is-mvc)
    - [Model](#model)
    - [View](#view)
    - [Controller](#controller)
    - [MVC Request Flow](#mvc-request-flow)
    - [Spring MVC Controller vs GRASP Controller](#spring-mvc-controller-vs-grasp-controller)
  - [Facade Design Pattern Analysis](#facade-design-pattern-analysis)
    - [Facade Intent](#facade-intent)
    - [Facade Participants](#facade-participants)
    - [Why the Facade Is Necessary](#why-the-facade-is-necessary)
    - [What the Facade Should and Should Not Own](#what-the-facade-should-and-should-not-own)
    - [Facade Request Flow](#facade-request-flow)
    - [Facade Benefits](#facade-benefits)
  - [GRASP, SOLID, and Refactoring Analysis](#grasp-solid-and-refactoring-analysis)
    - [GRASP](#grasp)
    - [SOLID](#solid)
    - [Refactoring](#refactoring)
  - [Data Structures Used](#data-structures-used)
    - [Why a Priority Queue Instead of a Normal Queue](#why-a-priority-queue-instead-of-a-normal-queue)
    - [Why `arrivalSequence` Is Necessary](#why-arrivalsequence-is-necessary)
  - [Big-O Analysis](#big-o-analysis)
    - [Waiting Emergency Calls](#waiting-emergency-calls)
    - [Ambulance and Dispatch Lookup](#ambulance-and-dispatch-lookup)
    - [Availability Tracking](#availability-tracking)
    - [Dispatch History](#dispatch-history)
    - [Candidate Evaluation](#candidate-evaluation)
    - [End-to-End Dispatch Complexity](#end-to-end-dispatch-complexity)
  - [Installation](#installation)
    - [Prerequisites](#prerequisites)
    - [Clone the Project](#clone-the-project)
    - [Run the Backend](#run-the-backend)
    - [Run the Frontend](#run-the-frontend)
  - [AI Usage](#ai-usage)


## Design Process




## Assumptions and Open Questions




## Design Decision Log

| Decision | Alternatives Considered | Rationale |
|---|---|---|
| Use PriorityQueue<EmergencyCall> for waiting EmergencyCalls. | Considered a normal FIFO Queue, a sorted List, and a custom MinHeap implementation. | A FIFO Queue cannot move a later CRITICAL call ahead of a lower-priority call. Java PriorityQueue already provides heap-backed priority behavior and satisfies the data-structure requirement without maintaining a separate production heap implementation. |
| Use EmergencyCallComparator to define waiting-call precedence. | Considered placing all ordering logic directly inside EmergencyCall or sorting calls manually every time the next call was needed. | A dedicated comparator keeps queue-ordering rules in one cohesive class and makes the ordering policy explicit and testable. |
| Use Priority first and arrivalSequence second. | Considered ordering only by Priority or only by timestamp. | Priority alone does not resolve equal-priority calls, while timestamp alone does not guarantee medical urgency. arrivalSequence preserves deterministic FCFS behavior even when timestamps are identical. |
| Keep ACTIVE/on-duty separate from AVAILABLE. | Considered one boolean or one status representing both ideas. | An Ambulance can be staffed and active but already dispatched, transporting, refueling, or otherwise unable to accept another EmergencyCall. |
| Use AmbulanceCallCenter as the aggregate root. | Considered allowing Controllers and services to manipulate the waiting queue, fleet map, available set, active Dispatch records, recommendations, and history directly. | AmbulanceCallCenter owns the major collections and can enforce cross-object invariants such as no double dispatch, consistent availability, and correct queue mutation. |
| Use AmbulanceDispatchFacade as the application-level GRASP Controller. | Considered letting DispatchController coordinate AmbulanceCallCenter, CadRecommendationService, and RouteService directly. | The Facade keeps the MVC Controller thin and provides a focused application boundary for dispatch workflows. |
| Keep DispatchController as a thin MVC Controller. | Considered placing queue logic, ambulance selection, revalidation, and dispatch mutation inside the Spring Controller. | The MVC Controller should receive HTTP requests and return responses, not own the business rules of dispatch. |
| Use CadRecommendationService for candidate ranking. | Considered putting ranking behavior inside Ambulance, EmergencyCall, or DispatchController. | Ranking is a separate responsibility that evaluates candidate data and should not make domain entities or web Controllers unnecessarily broad. |
| Use immutable CAD context for recommendation work. | Considered allowing CadRecommendationService to read and mutate live AmbulanceCallCenter state. | Immutable candidate snapshots reduce coupling and prevent external travel-estimate work from directly modifying authoritative domain state. |
| Keep DispatchRecommendation separate from DispatchRecord. | Considered creating a DispatchRecord immediately when a recommendation is generated. | A recommendation is only a proposal. The actual Dispatch should not exist until the Emergency Dispatcher approves or overrides the recommendation and the system revalidates the Ambulance. |
| Revalidate the Ambulance before committing the Dispatch. | Considered trusting the earlier recommendation without checking again. | An Ambulance may become unavailable between recommendation and confirmation. Revalidation prevents stale recommendations and double assignment. |
| Keep Ambulance Crew acknowledgement as a separate use case. | Considered making acknowledgement the final step of Dispatch Ambulance. | The Emergency Dispatcher controls the dispatch-confirmation use case, while the Ambulance Crew performs a separate user goal after the assignment exists. |
| Keep Hospital out of the Dispatch Ambulance sequence. | Considered including all domain entities on every diagram. | Hospital is not needed during initial dispatch and should only appear when a use case actually requires a transport destination. |
| Group use cases into functional packages. | Considered one large use-case diagram containing every Emergency Dispatcher, Ambulance Crew, Fleet Supervisor, and Administrator action. | Functional grouping keeps diagrams understandable and avoids an oversized flat use-case list. |
| Use TravelEstimateProvider and RouteProvider interfaces. | Considered depending directly on Google Routes from application services. | Provider interfaces isolate external technology and support Dependency Inversion and Protected Variations. |
| Number only method calls on sequence diagrams. | Considered numbering returns, object values, loop labels, alt guards, and state descriptions. | Numbering only actual calls makes the sequence easier to trace to receiving class operations. |
| Show return arrows only when they add value. | Considered showing a return for every method call. | Most return arrows add visual noise. Only returned values that are used later need to be shown. |


## UML and OOAD Artifact Analysis


### Domain Model


<img width="743" height="508" alt="image" src="https://github.com/user-attachments/assets/2938d8c6-5c5f-4352-89e7-bc1727205e96" />




### Use-Case Scenario







## Use Cases

<img width="1100" height="867" alt="image" src="https://github.com/user-attachments/assets/b5f2b119-c0ee-4db9-af3c-48752537a533" />

<img width="528" height="905" alt="image" src="https://github.com/user-attachments/assets/7571746e-a7ca-4a2a-a9b7-b765f6e79b6f" />


### Dispatch Ambulance






### Classes

Class: AmbulanceCallCenter

- waitingCalls : PriorityQueue<EmergencyCall>
- fleetById : HashMap<Integer, Ambulance>
- availableAmbulanceIds : HashSet<Integer>
- activeDispatchesByAmbulanceId : HashMap<Integer, DispatchRecord>
- recommendationsById : HashMap<Long, DispatchRecommendation>
- dispatchHistory : ArrayList<DispatchRecord>
- statistics : SystemStatisticsAccumulator

+ cadRecommendationContext() : CadRecommendationContextSnapshot
+ createRecommendation(callId : long, ambulanceId : int, estimate : TravelEstimate) : DispatchRecommendationSnapshot
+ approveRecommendation(recommendationId : long) : DispatchBatchSnapshot
+ overrideRecommendation(recommendationId : long, ambulanceId : int, reason : String) : DispatchBatchSnapshot
+ acknowledgeDispatch(ambulanceId : int) : AmbulanceOperationSnapshot
+ boardSnapshot() : BoardSnapshot
+ statisticsSnapshot() : StatisticsSnapshot

Class: EmergencyCall

- callId : long
- currentPriority : Priority
- location : Location
- requiredCapability : ClinicalCapability
- jurisdiction : String
- mutualAidAllowed : boolean
- arrivalSequence : long

+ getCurrentPriority() : Priority
+ getLocation() : Location
+ getRequiredCapability() : ClinicalCapability
+ getJurisdiction() : String
+ isMutualAidAllowed() : boolean

Class: EmergencyCallComparator

+ compare(left : EmergencyCall, right : EmergencyCall) : int

Class: Ambulance

- ambulanceId : int
- name : String
- location : Location
- status : AmbulanceStatus
- dutyStatus : DutyStatus
- capability : ClinicalCapability
- jurisdiction : String
- activeCall : EmergencyCall

+ getAmbulanceId() : int
+ getName() : String
+ getLocation() : Location
+ getStatus() : AmbulanceStatus
+ getDutyStatus() : DutyStatus
+ getCapability() : ClinicalCapability
+ getJurisdiction() : String
+ isAvailable() : boolean
+ isActiveOnDuty() : boolean
+ isAppropriateFor(requiredCapability : ClinicalCapability, requiredJurisdiction : String, mutualAidAllowed : boolean) : boolean
+ assignTo(call : EmergencyCall, at : Instant) : void
+ acknowledgeDispatch(at : Instant) : void
+ snapshot() : AmbulanceSnapshot

Class: DispatchRecommendation

- recommendationId : long
- callId : long
- recommendedAmbulanceId : int
- travelEstimate : TravelEstimate
- createdAt : Instant

+ recommendationId() : long
+ callId() : long
+ recommendedAmbulanceId() : int
+ travelEstimate() : TravelEstimate
+ createdAt() : Instant

Class: DispatchRecord

- dispatchId : long
- call : EmergencyCall
- ambulanceId : int
- recommendedAmbulanceId : int
- dispatchedAt : Instant
- dispatcherOverride : boolean
- overrideReason : String
- acknowledgedAt : Instant
- arrivedOnSceneAt : Instant
- transportStartedAt : Instant
- arrivedAtHospitalAt : Instant
- completedAt : Instant

+ getDispatchId() : long
+ getCall() : EmergencyCall
+ getAmbulanceId() : int
+ getRecommendedAmbulanceId() : int
+ getDispatchedAt() : Instant
+ canRecordAcknowledgement(at : Instant) : boolean
+ recordAcknowledged(at : Instant) : void
+ snapshot() : DispatchSnapshot

Class: Location

- latitude : double
- longitude : double

+ distanceTo(other : Location) : double
+ squaredDistanceTo(other : Location) : double

Class: AmbulanceDispatchFacade

+ recommendNext() : DispatchRecommendationSnapshot
+ approveRecommendation(recommendationId : long) : DispatchBatchSnapshot
+ overrideRecommendation(recommendationId : long, ambulanceId : int, reason : String) : DispatchBatchSnapshot
+ acknowledgeDispatch(ambulanceId : int) : AmbulanceOperationSnapshot

Class: CadRecommendationService

+ recommend(context : CadRecommendationContextSnapshot) : CadRecommendationDecision

Class: DispatchController

+ recommendNext()
+ approve(recommendationId)
+ override(recommendationId, request)

Class: DispatchWebMapper

+ toRecommendationResponse(snapshot)
+ toDispatchBatchResponse(snapshot)


## Application Flow







### BDD Scenarios

Feature: Stable emergency call ordering

Scenario: Priority is considered before first come first served  
  Given multiple EmergencyCalls are waiting  
  When the calls have different Priority values  
  Then the system handles the higher medical Priority first  

Scenario: Equal-priority calls remain first come first served  
  Given two EmergencyCalls have the same Priority  
  When the calls are received in different arrival order  
  Then the call with the smaller arrivalSequence remains first  

Scenario: Same timestamp still preserves deterministic ordering  
  Given two EmergencyCalls have the same Priority and timestamp  
  When both calls are inserted into the waiting queue  
  Then arrivalSequence determines which call is handled first  


Feature: CAD ambulance recommendation

Scenario: Dispatcher approves the CAD recommendation  
  Given an EmergencyCall is waiting for dispatch  
  And an appropriate Ambulance is available  
  When the system recommends the Ambulance  
  And the Emergency Dispatcher approves the recommendation  
  Then the Ambulance is assigned to the EmergencyCall  
  And the system creates a DispatchRecord  

Scenario: No eligible ambulance keeps the call waiting  
  Given an EmergencyCall is waiting for dispatch  
  And no Ambulance satisfies the response requirements  
  When the Emergency Dispatcher requests a recommendation  
  Then the EmergencyCall remains waiting  

Scenario: Dispatcher overrides the recommendation  
  Given the system has recommended an Ambulance  
  And another appropriate Ambulance is available  
  When the Emergency Dispatcher selects the alternate Ambulance  
  Then the system revalidates the alternate Ambulance  
  And the system records the override when the Dispatch is committed  


Feature: Guarded ambulance lifecycle

Scenario: Crew acknowledges a dispatch before responding  
  Given an Ambulance has a committed Dispatch  
  When the Ambulance Crew acknowledges the Dispatch  
  Then the system records the acknowledgement  
  And the Ambulance transitions from DISPATCHED to EN_ROUTE  

Scenario: Crew cannot begin transport before arriving on scene  
  Given the Ambulance has not reached the emergency scene  
  When the Ambulance Crew attempts to begin transport  
  Then the system rejects the illegal transition  

Scenario: Complete an emergency without transport  
  Given the Ambulance Crew is ON_SCENE  
  When the crew completes the emergency without patient transport  
  Then the system records the no-transport completion  
  And the Ambulance begins returning to service  


## TDD Traceability to Methods






### View



### Controller



### MVC Request Flow




## Facade Design Pattern Analysis

### Facade Intent



### Facade Participants


### Why the Facade Is Necessary




### Facade Request Flow


## GRASP, SOLID, and Refactoring Analysis

### GRASP


### SOLID




## Data Structures Used

The project intentionally uses different data structures for different access patterns.

| Data Structure | Field / Use | Why It Fits the Problem |
|---|---|---|
| `PriorityQueue<EmergencyCall>` | `waitingCalls` | The system repeatedly needs the highest-precedence waiting emergency rather than simply the oldest call. A heap-backed priority queue makes the next call efficient to retrieve. |
| `EmergencyCallComparator` | Priority queue ordering policy | Encapsulates the comparison rule: medical priority first, `arrivalSequence` second. |
| `HashMap<Integer, Ambulance>` | `fleetById` | Ambulances are frequently located by unique ID. Hash lookup is more appropriate than scanning a list for every access. |
| `HashSet<Integer>` | `availableAmbulanceIds` | Availability is fundamentally a membership question: is this ambulance currently in the available set? |
| `HashMap<Integer, DispatchRecord>` | `activeDispatchesByAmbulanceId` | Supports direct lookup of the active dispatch associated with a particular ambulance. |
| `HashMap<Long, DispatchRecommendation>` | `recommendationsById` | Allows a dispatcher approval or override request to locate the pending recommendation by recommendation ID. |
| `ArrayList<DispatchRecord>` | `dispatchHistory` | Dispatch history is append-oriented and benefits from efficient ordered storage and indexed traversal. |

### Why a Priority Queue Instead of a Normal Queue



### Why `arrivalSequence` Is Necessary



## Big-O Analysis

Let:

- `n` = number of waiting emergency calls
- `a` = number of ambulances being considered
- `r` = number of pending recommendations
- `d` = number of active dispatches
- `h` = number of historical dispatch records

### Waiting Emergency Calls

Java `PriorityQueue` is heap-backed.

| Operation | Expected Complexity | Dispatch Meaning |
|---|---:|---|
| `peek()` | `O(1)` | Inspect the next emergency call without removing it. |
| `offer()` / `add()` | `O(log n)` | Add a newly evaluated emergency call while restoring heap order. |
| `poll()` | `O(log n)` | Remove the highest-precedence emergency call and restore heap order. |
| Arbitrary search by call ID | `O(n)` if iteration is used | A heap is optimized for the root element, not arbitrary lookup. |
| Arbitrary removal | `O(n)` search + heap repair | Removing a non-root item requires locating it first. |

The important design tradeoff is that the application optimizes the operation it performs most conceptually: **determine the next call to handle**.

If a sorted list were used instead, either insertion or repeated sorting would become more expensive. If a plain FIFO queue were used, the complexity could be good while the dispatch semantics would be wrong.

### Ambulance and Dispatch Lookup

`HashMap` is used where the system knows an identifier and needs the associated object.

Typical average-case complexity is:

| Operation | Average | Worst Case |
|---|---:|---:|
| `HashMap.get(key)` | `O(1)` | `O(n)` theoretical worst case |
| `HashMap.put(key, value)` | `O(1)` amortized | `O(n)` theoretical worst case |
| `HashMap.remove(key)` | `O(1)` average | `O(n)` theoretical worst case |

This applies to structures such as:

```text
fleetById
activeDispatchesByAmbulanceId
recommendationsById
```

Direct hash lookup is preferable to repeatedly scanning an `ArrayList` of ambulances or recommendations, which would require `O(a)` or `O(r)` search time.

### Availability Tracking

`availableAmbulanceIds` is a `HashSet<Integer>`.

Typical average-case operations are:

```text
contains(id)  → O(1)
add(id)       → O(1) amortized
remove(id)    → O(1) average
```

This makes the set appropriate for fast availability membership tracking.

The set does not replace the `Ambulance` object as the authority on whether a transition is valid. It is a supporting index that must remain consistent with domain state.

### Dispatch History

`dispatchHistory` is an `ArrayList<DispatchRecord>`.

Typical costs are:

```text
append to end       → O(1) amortized
get by index        → O(1)
iterate all history → O(h)
search by predicate → O(h)
insert in middle    → O(h)
```

An `ArrayList` fits an append-heavy history because completed dispatches are naturally retained in sequence and commonly reviewed by iteration.

### Candidate Evaluation

Identifying an appropriate ambulance may require examining the candidate fleet. Suitability checks such as availability, duty status, capability, and jurisdiction are constant-time checks per ambulance when the required values are already in memory.

A full scan of `a` ambulance candidates is therefore approximately:

```text
O(a)
```

Travel-estimate calls add external I/O cost that Big-O notation does not represent well. From the local algorithm's perspective, evaluating each candidate is linear in the number of candidates, but real elapsed time may be dominated by route-provider latency.

The exact complexity of `CadRecommendationService.recommend(...)` depends on its implementation. If it scans candidates while retaining only the current best choice, it is `O(a)`. If it sorts all candidates before selecting the first, it is `O(a log a)`. The current design documentation establishes candidate comparison but does not by itself prove which of those two internal strategies the implementation uses.

### End-to-End Dispatch Complexity

For the core waiting-call operation:

```text
peek next call             O(1)
scan/evaluate ambulances   O(a)
lookup recommendation      O(1) average
lookup selected ambulance  O(1) average
commit/removal from heap   O(log n) when removing the root
append dispatch history    O(1) amortized
```

Ignoring external routing latency, a simplified successful dispatch is therefore dominated by candidate evaluation plus heap mutation:

```text
O(a + log n)
```

That expression assumes the emergency being committed is the root/next item in the priority queue and candidate selection is implemented as a linear best-choice scan. If the code searches for an arbitrary waiting call or sorts all ambulance candidates, the bound changes accordingly.

This analysis demonstrates an important design lesson: Big-O should be applied to the operation actually being performed, not merely attached to the name of a data structure.


## Installation


### Prerequisites

Before running the application, make sure the following software is installed:

- Java Development Kit (JDK) 21 or later
- Maven
- Node.js
- npm
- Git
- IntelliJ IDEA, Eclipse, VS Code, or another Java-compatible IDE

Optional:

- Google Routes API key for Google-backed route and travel estimates


### Clone the Project

```bash
git clone <repository-url>
cd ambulance-dispatch-system
```


### Run the Backend

```bash
mvn spring-boot:run
```

Run the Java tests:

```bash
mvn test
```


### Run the Frontend

```bash
cd frontend
npm install
npm run dev
```

Build the frontend:

```bash
npm run build
```


## AI Usage

AI was used as a design and review assistant during parts of the requirements analysis, noun analysis, domain modeling, use-case modeling, robustness analysis, sequence-diagram review, MVC/GRASP/SOLID review, data-structure analysis, testing, and documentation work.

Course-required ChatGPT share links can be added below:

https://chatgpt.com/share/<add-link>  
https://chatgpt.com/share/<add-link>  
https://chatgpt.com/share/<add-link>
