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

I used an object-oriented and use-case-driven design approach by first understanding the ambulance dispatch problem before writing or refining the implementation. I identified the major behaviors the system needed to support: accepting and evaluating EmergencyCalls, assigning Priority, maintaining waiting-call order, identifying appropriate Ambulances, determining the best available response, allowing the Emergency Dispatcher to approve or override a recommendation, recording a Dispatch, supporting Ambulance Crew response states, managing fleet availability, and reviewing operational history.

The development process started with noun analysis and domain modeling. I reviewed the requirements and extracted important nouns such as EmergencyCall, Ambulance, Dispatch, Location, Priority, Hospital, Emergency Dispatcher, Ambulance Crew, Fleet Supervisor, Administrator, ClinicalCapability, jurisdiction, mutual aid, availability, recommendation, response status, and travel estimate. I then evaluated each noun by asking whether it represented a meaningful object with state, behavior, and responsibility within the system.

The first-pass domain model focused on six major business concepts: EmergencyCall, Ambulance, Dispatch, Location, Priority, and Hospital. Supporting software objects such as AmbulanceCallCenter, DispatchRecommendation, DispatchRecord, EmergencyCallComparator, AmbulanceDispatchFacade, CadRecommendationService, DispatchController, and TravelEstimateProvider were introduced later during detailed design.

The use cases were organized into functional groups instead of placing every use case on one large diagram. The major groups are Emergency Intake, Dispatch, Resource Coordination, Field Response, Fleet Operations, and Administration. This keeps related behavior together and prevents the Emergency Dispatcher from having an excessively large flat list of use cases.

The use-case text is written in active voice using an event/response flow. The actor performs an action, the system responds, and the use-case text describes both sides of the interaction. The use cases use the agreed domain vocabulary but do not contain implementation details such as PriorityQueue, HashMap, REST endpoints, Java methods, Controllers, or Facades.

After the use cases were written, robustness analysis was used to connect use-case behavior to boundary, control, and entity responsibilities. The Dispatch Ambulance robustness analysis showed that Ambulance is the primary entity during resource selection, while Dispatch becomes the primary business entity after the Emergency Dispatcher confirms the assignment.

Sequence diagrams were then used to allocate behavior to concrete classes and methods. Method calls are numbered only with whole-number integers. Return values are not numbered, and return arrows are only shown when they add useful information. Loop and alternate behavior are shown inside UML combined-fragment boxes.

The implementation was then refined using Java, Spring Boot, React, JUnit, Cucumber, REST testing, and architecture checks. The project therefore moves from requirements analysis, to noun analysis, to domain modeling, to use cases, to robustness analysis, to sequence diagrams, to class design, to implementation, and finally to automated testing.


## Assumptions and Open Questions

Several assumptions were made because the project does not define every operational rule that would exist in a real emergency communications center.

I assumed that the Emergency Dispatcher is the primary actor responsible for ambulance dispatch. The system may also coordinate Fire, Police, mutual-aid EMS, specialized response teams, emergency management, or other emergency-support resources, but ambulance dispatch remains the main workflow being modeled.

I assumed that ACTIVE/on-duty and AVAILABLE are separate concepts. An Ambulance may be active and staffed while still being unavailable because it is already responding to another EmergencyCall or temporarily unable to accept another assignment.

I assumed that the CAD system recommends an Ambulance rather than automatically making the final dispatch decision. The Emergency Dispatcher reviews the recommended Ambulance and may accept the recommendation or select another appropriate Ambulance.

I assumed that equal-priority EmergencyCalls must remain first come, first served. Because two calls can share the same Priority and timestamp, arrivalSequence is used as the final deterministic tie-break.

I assumed that DispatchRecommendation and DispatchRecord represent two different concepts. DispatchRecommendation represents the proposed assignment before approval. DispatchRecord represents the committed assignment after the Emergency Dispatcher confirms the Dispatch.

I assumed that Ambulance Crew acknowledgement is a separate use case from Dispatch Ambulance. Dispatch Ambulance ends after the assignment is confirmed and recorded. The Ambulance Crew then acknowledges the Dispatch and begins responding.

I also assumed that Hospital is part of the overall domain but is not involved in the initial Dispatch Ambulance use case. Hospital becomes relevant later if the Ambulance Crew transports a patient.

In a real client engagement, I would ask whether dispatchers can override clinical capability rules, whether some incident types automatically require Fire or Police response, when mutual aid may be used, how long a crew may remain unacknowledged before escalation, whether an en-route Ambulance may be diverted to a higher-priority EmergencyCall, and how real-time traffic or road closures should influence recommendations.


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

**Question answered:** What happens step by step when the actor performs a system goal?

The detailed **Dispatch Ambulance** scenario provides the behavioral contract that later diagrams must realize.

The main success flow establishes that the system:

1. receives a dispatch-ready emergency,
2. identifies appropriate ambulances,
3. determines the best recommendation,
4. presents the recommendation,
5. allows the dispatcher to accept or select another ambulance,
6. revalidates the selected ambulance,
7. receives confirmation, and
8. records the dispatch.

The alternate flows are equally important because they define required behavior when no ambulance is available, a recommendation becomes stale, another ambulance is selected, the emergency no longer needs a response, or the dispatcher does not confirm.

The scenario remains implementation-neutral. It says **what the system must do**, not that a `HashMap` or a specific Java method performs the step.

### Robustness Diagram

**Question answered:** Which boundary, control, and entity responsibilities are needed to realize the use-case scenario?

Robustness analysis is the bridge between use-case text and detailed object design.

For Dispatch Ambulance, the analysis separates:

- **Boundary:** dispatcher-facing interaction such as `DispatchWorkspace`
- **Control:** coordination of the dispatch use case
- **Entity:** `EmergencyCall`, `Ambulance`, `DispatchRecommendation`, and `Dispatch`/`DispatchRecord`

The robustness analysis helps prevent responsibility mistakes. For example, the user-interface boundary should not determine whether an ambulance is appropriate, and an entity should not directly manage HTTP input.

In the selection portion of the workflow, `Ambulance` is the primary entity because it owns the information needed to determine availability and suitability. After confirmation, `Dispatch` becomes the important business relationship and `DispatchRecord` becomes the committed Java representation.

### Sequence Diagram

**Question answered:** Which concrete objects call which operations, and in what order, to implement the scenario?

The sequence diagram is where the conceptual behavior becomes source-aligned method collaboration.

The project traces the request through participants such as:

```text
Emergency Dispatcher
DispatchWorkspace
DispatchController
AmbulanceDispatchFacade
AmbulanceCallCenter
CadRecommendationService
TravelEstimateProvider
EmergencyCall
Ambulance
DispatchRecommendation
DispatchRecord
```

This diagram is especially important for demonstrating MVC and Facade because the message order makes the architectural boundary visible:

```text
Boundary → MVC Controller → Facade → Domain/Application Subsystem
```

It also demonstrates where revalidation occurs before dispatch commitment. The sequence diagram should therefore agree with the real method signatures in the implementation.

### Class Diagram

**Question answered:** What classes exist in the detailed software design, what state and operations do they own, and how are they related?

The class diagram is more implementation-specific than the domain model. It contains software classes that did not belong in the first-pass domain model, including:

```text
AmbulanceCallCenter
EmergencyCallComparator
AmbulanceDispatchFacade
CadRecommendationService
DispatchController
DispatchWebMapper
DispatchRecommendation
DispatchRecord
```

This is also the UML artifact where the project's data-structure choices become visible through fields such as:

```text
PriorityQueue<EmergencyCall>
HashMap<Integer, Ambulance>
HashSet<Integer>
HashMap<Integer, DispatchRecord>
HashMap<Long, DispatchRecommendation>
ArrayList<DispatchRecord>
```

The class diagram therefore demonstrates the convergence of OOAD, MVC architecture, Facade, and data-structure implementation.

### How the UML Artifacts Connect

The artifacts intentionally increase in technical detail:

| Artifact | Main Question | Technology Detail |
|---|---|---|
| Domain Model | What concepts exist? | Very low |
| Use-Case Diagram | Who wants what from the system? | Very low |
| Use-Case Scenario | What behavior must occur? | Low |
| Robustness Diagram | What boundary/control/entity responsibilities are required? | Medium |
| Sequence Diagram | Which concrete objects/methods collaborate? | High |
| Class Diagram | What is the final static software structure? | High |

The **Dispatch Ambulance** thread can be followed across all of them:

```text
Domain concept: EmergencyCall + Ambulance + Dispatch
        ↓
Use-case goal: Dispatch Ambulance
        ↓
Scenario: identify → recommend → review → revalidate → confirm → record
        ↓
Robustness: Boundary → Control → Entities
        ↓
Sequence: DispatchController → AmbulanceDispatchFacade → AmbulanceCallCenter / services / entities
        ↓
Class design: MVC classes + Facade + domain objects + data structures
        ↓
Implementation: Spring Boot + Java collections
        ↓
Tests: BDD + TDD + REST/API + architecture checks
```

This traceability is one of the main strengths of the project because the implementation can be explained from both the software-architecture perspective and the data-structures perspective.


## Noun Analysis

The noun analysis started by identifying important nouns from the emergency call and ambulance dispatch requirements.

Important nouns included:

EmergencyCall  
Ambulance  
Dispatch  
DispatchRecommendation  
DispatchRecord  
Location  
Priority  
Hospital  
Emergency Dispatcher  
Ambulance Crew  
Fleet Supervisor  
Administrator  
ClinicalCapability  
jurisdiction  
mutual aid  
arrivalSequence  
waiting queue  
availability  
response status  
route  
travel estimate  
Fire Department  
Police Department  
Emergency Support Agency  

The nouns were evaluated by asking whether each concept needed meaningful state, behavior, identity, or lifecycle within the system.

The core business-domain objects are:

EmergencyCall  
Ambulance  
Dispatch  
Location  
Priority  
Hospital  

The primary actors are:

Emergency Dispatcher  
Ambulance Crew  
Fleet Supervisor  
Administrator  

Actors are not modeled as domain classes simply because they use the system. They represent external roles unless the application specifically needs to store and manage actor identity or account information.

DispatchRecommendation and DispatchRecord were introduced during detailed design to refine the business concept of Dispatch. ClinicalCapability, jurisdiction, mutual aid, duty status, ambulance status, and arrivalSequence are supporting values or types. PriorityQueue, HashMap, HashSet, Controllers, Facades, REST endpoints, and Google Routes are implementation concepts rather than first-pass domain objects.

![Noun Analysis](assets/noun-analysis.png)


## Domain Modeling

The domain model was created by identifying the meaningful entities that exist in the ambulance dispatch problem domain and validating whether each object has meaningful state and behavior.

The core domain objects are:

EmergencyCall  
Ambulance  
Dispatch  
Location  
Priority  
Hospital  

EmergencyCall represents an emergency incident that has been accepted and evaluated. It stores information such as the current Priority, incident Location, required ClinicalCapability, jurisdiction, mutual-aid permission, and intake ordering information. EmergencyCall provides the information needed to determine what kind of response is required.

Ambulance represents an EMS response resource. It stores duty status, operational status, ClinicalCapability, jurisdiction, current Location, active EmergencyCall, availability information, GPS position, and state history. Ambulance is responsible for determining whether it is available, whether it is appropriate for an EmergencyCall, and whether a requested lifecycle transition is legal.

Dispatch represents the relationship that assigns an Ambulance to an EmergencyCall. In the detailed Java design, DispatchRecommendation represents a proposed assignment before dispatcher approval, while DispatchRecord represents the committed assignment after approval.

Location represents a geographic position. It is used for the EmergencyCall location, Ambulance location, scene location, route calculations, and Hospital destination. Location also supports direct distance calculations when needed.

Priority represents the medical urgency of an EmergencyCall. Priority is used by EmergencyCallComparator when ordering waiting calls.

Hospital represents a patient transport destination. Hospital remains part of the overall business domain, but it is not involved in the initial Dispatch Ambulance use case. The current implementation represents hospital destinations with Location rather than a dedicated Hospital Java class.

This domain model keeps the first-pass business concepts separate from later software-design objects such as Controllers, Facades, recommendation services, repositories, DTOs, and Java collection classes.

![Domain Model](assets/domain-model.png)


## Use Cases

<img width="1100" height="867" alt="image" src="https://github.com/user-attachments/assets/b5f2b119-c0ee-4db9-af3c-48752537a533" />

<img width="528" height="905" alt="image" src="https://github.com/user-attachments/assets/7571746e-a7ca-4a2a-a9b7-b765f6e79b6f" />



The main use cases are organized into six functional areas:

Emergency Intake  
Dispatch  
Resource Coordination  
Field Response  
Fleet Operations  
Administration  

![Use Case Packages](assets/use-case-packages.png)


### Emergency Dispatcher

The Emergency Dispatcher participates in Emergency Intake, Dispatch, and Resource Coordination.

Major use cases include:

- Accept Emergency Call
- Evaluate Emergency Call
- Assign Priority
- Change Priority
- Determine Response Needs
- View Waiting Emergencies
- View Available Ambulances
- Dispatch Ambulance
- Review Ambulance Recommendation
- Override Ambulance Recommendation
- Monitor Active Dispatches
- Handle Unacknowledged Dispatch
- Reassign Emergency Resource
- Request Ambulance Response
- Request Fire Response
- Request Police Response
- Request Additional EMS
- Authorize Mutual Aid
- Request Specialized Response
- Request Emergency Support

![Emergency Dispatcher Use Cases](assets/emergency-dispatcher-use-cases.png)


### Dispatch Ambulance

Primary Actor: Emergency Dispatcher

The Emergency Dispatcher dispatches an appropriate Ambulance to an evaluated EmergencyCall. The system identifies eligible Ambulances, determines the best available choice, presents a recommendation, allows the Emergency Dispatcher to approve or choose another appropriate Ambulance, revalidates the selected Ambulance, and records the confirmed Dispatch.

Main flow:

1. The Emergency Dispatcher completes the EmergencyCall evaluation and indicates that the EmergencyCall is ready for dispatch.
2. The system identifies Ambulances that are currently available and appropriate for the EmergencyCall.
3. The system determines which appropriate Ambulance can provide the best response and presents that Ambulance as the recommendation.
4. The Emergency Dispatcher reviews the recommended Ambulance.
5. The Emergency Dispatcher accepts the recommended Ambulance or selects another appropriate Ambulance.
6. The system verifies that the selected Ambulance is still eligible and that the EmergencyCall still requires a response.
7. The Emergency Dispatcher confirms the Dispatch.
8. The system records the Dispatch between the EmergencyCall and the selected Ambulance.

Alternative flows:

- If no appropriate Ambulance is currently available, the system keeps the EmergencyCall waiting for dispatch.
- If the Emergency Dispatcher selects another Ambulance, the system validates the alternate Ambulance before commitment.
- If the selected Ambulance is no longer available or appropriate, the system rejects the stale selection and provides another option.
- If the EmergencyCall no longer requires dispatch, the system does not create a DispatchRecord.
- If the Emergency Dispatcher does not confirm the Dispatch, the system does not commit the assignment.

![Dispatch Ambulance Use Case](assets/dispatch-ambulance-use-case.png)

The robustness analysis for Dispatch Ambulance maps each behavior to the entity that owns or supplies the required information.

| Robustness Behavior | Primary Entity | Supporting Entities |
|---|---|---|
| Identify Ambulances | Ambulance | EmergencyCall, Priority, Location |
| Determine Best Choice | Ambulance | EmergencyCall, Priority, Location |
| Review Recommended Ambulance | Ambulance | EmergencyCall, DispatchRecommendation |
| Dispatcher Accepts or Chooses Another | Ambulance | EmergencyCall, DispatchRecommendation |
| Revalidate Selected Ambulance | Ambulance | EmergencyCall, DispatchRecommendation |
| Confirm Dispatch | Dispatch / DispatchRecord | EmergencyCall, Ambulance |
| Record Confirmed Assignment | DispatchRecord | EmergencyCall, Ambulance |

Ambulance is the primary entity during resource selection because it owns availability, duty status, capability, jurisdiction, Location, and assignment state. After the dispatcher confirms the assignment, Dispatch becomes the primary business entity and DispatchRecord becomes the concrete Java object representing the committed assignment.

Hospital does not participate in Dispatch Ambulance because no patient transport destination has been selected yet.

![Dispatch Ambulance Robustness Diagram](assets/dispatch-ambulance-robustness.png)

The Dispatch Ambulance sequence diagram allocates the robustness behavior to concrete classes and methods. Only actual method calls are numbered, and the final diagram uses whole-number integers only. Return arrows are not numbered. Loop and alternate behavior are contained inside UML combined-fragment boxes.

The detailed design uses the following participants:

Emergency Dispatcher  
DispatchWorkspace  
DispatchController  
AmbulanceDispatchFacade  
AmbulanceCallCenter  
CadRecommendationService  
TravelEstimateProvider  
EmergencyCall  
Ambulance  
DispatchRecommendation  
DispatchRecord  

The source-aligned method flow includes:

1. DispatchController.recommendNext()
2. AmbulanceDispatchFacade.recommendNext()
3. AmbulanceCallCenter.cadRecommendationContext()
4. EmergencyCall.getCurrentPriority()
5. EmergencyCall.getLocation()
6. EmergencyCall.getRequiredCapability()
7. EmergencyCall.getJurisdiction()
8. EmergencyCall.isMutualAidAllowed()
9. Ambulance.isAvailable()
10. Ambulance.isActiveOnDuty()
11. Ambulance.isAppropriateFor(...)
12. Ambulance.snapshot()
13. CadRecommendationService.recommend(context)
14. CadRecommendationService.estimate(origin, destination)
15. TravelEstimateProvider.computeTravelEstimate(origin, destination)
16. AmbulanceCallCenter.createRecommendation(...)
17. AmbulanceCallCenter.recommendationSnapshot(...)
18. DispatchWebMapper.toRecommendationResponse(...)
19. DispatchController.approve(recommendationId)
20. AmbulanceDispatchFacade.approveRecommendation(recommendationId)
21. AmbulanceCallCenter.approveRecommendation(recommendationId)
22. AmbulanceCallCenter.requireRecommendation(...)
23. DispatchRecommendation.callId()
24. DispatchRecommendation.recommendedAmbulanceId()
25. AmbulanceCallCenter.findWaitingCall(...)
26. AmbulanceCallCenter.requireAmbulance(...)
27. AmbulanceCallCenter.dispatchSpecific(...)
28. Ambulance.isAppropriateFor(...)
29. Ambulance.assignTo(call, dispatchedAt)
30. AmbulanceCallCenter.dispatchBatch(record)
31. DispatchRecord.snapshot()
32. DispatchWebMapper.toDispatchBatchResponse(...)

DispatchRecord is created during the successful approval path. If the selected Ambulance is no longer eligible or the EmergencyCall no longer requires dispatch, the successful mutation does not occur.

![Dispatch Ambulance Sequence Diagram](assets/dispatch-ambulance-sequence.png)


### Ambulance Crew

The Ambulance Crew participates in the Field Response use cases.

Major use cases include:

- View Assigned Emergency
- Acknowledge Dispatch
- View Route to Emergency
- Update Ambulance Location
- Report Arrival on Scene
- Manage On-Scene Response
- Manage Patient Transport
- Complete Emergency Response
- Return Ambulance to Service

Acknowledge Dispatch is separate from Dispatch Ambulance. After a DispatchRecord exists, the Ambulance Crew acknowledges the committed Dispatch. The system records the acknowledgement and the Ambulance transitions from DISPATCHED to EN_ROUTE.

![Ambulance Crew Use Cases](assets/ambulance-crew-use-cases.png)


### Fleet Supervisor

The Fleet Supervisor participates in Fleet Operations.

Major use cases include:

- View Ambulance Fleet
- Register Ambulance
- Manage Ambulance Availability
- Manage Refueling
- Manage Ambulance Maintenance
- Mark Ambulance Unstaffed
- Take Ambulance Out of Service
- Restore Ambulance to Service
- Review Ambulance Status History

![Fleet Supervisor Use Cases](assets/fleet-supervisor-use-cases.png)


### Administrator

The Administrator participates in Administration.

Major use cases include:

- Review System Statistics
- Review Dispatch History
- Review Ambulance Utilization
- Review Emergency Call Activity
- Review Operational Performance

![Administrator Use Cases](assets/administrator-use-cases.png)


## UML Class Diagram

![UML Class Diagram](assets/class-diagram.png)


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

The application begins when the Emergency Dispatcher receives and evaluates an EmergencyCall.

The system assigns the EmergencyCall a Priority and response requirements. The EmergencyCall is then placed into the waiting PriorityQueue. EmergencyCallComparator orders waiting calls by Priority and then arrivalSequence.

When the Emergency Dispatcher starts the Dispatch Ambulance workflow, the system identifies eligible Ambulances and builds a CAD recommendation context. CadRecommendationService compares the candidate information and travel estimates and returns the best recommendation. The system stores the proposed assignment as a DispatchRecommendation and presents it to the Emergency Dispatcher.

The Emergency Dispatcher can approve the recommended Ambulance or choose another appropriate Ambulance. Before committing the assignment, the system revalidates the EmergencyCall and Ambulance. If the selection is still valid, the Ambulance is assigned and the system creates a DispatchRecord.

The Ambulance Crew then uses a separate Acknowledge Dispatch workflow. After acknowledgement, the Ambulance can move through the response lifecycle from DISPATCHED to EN_ROUTE, ON_SCENE, TRANSPORTING, AT_HOSPITAL, and eventually back to AVAILABLE.

The core application flow is:

1. Accept EmergencyCall.
2. Evaluate EmergencyCall.
3. Assign Priority and response requirements.
4. Add EmergencyCall to waiting PriorityQueue.
5. Select the next waiting EmergencyCall.
6. Identify eligible Ambulances.
7. Determine the best Ambulance.
8. Create DispatchRecommendation.
9. Emergency Dispatcher reviews recommendation.
10. Emergency Dispatcher approves or overrides.
11. Revalidate EmergencyCall and Ambulance.
12. Assign Ambulance.
13. Create DispatchRecord.
14. Ambulance Crew acknowledges Dispatch.
15. Continue the Ambulance response lifecycle.
16. Complete the Dispatch and retain history.

![Application Flow](assets/application-flow.png)


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

TDD was used to verify the individual methods and classes that implement waiting-call ordering, ambulance eligibility, CAD recommendation, dispatcher approval, ambulance lifecycle rules, location updates, and Dispatch chronology.

Class / Method                                      TDD Test
--------------------------------------------------------------------------------
EmergencyCallComparator.compare()                   criticalPrecedesHigh

EmergencyCallComparator.compare()                   equalPriorityUsesArrivalSequence

EmergencyCallComparator.compare()                   sameTimestampStillUsesArrivalSequence

PriorityQueue<EmergencyCall> ordering               higherPriorityIsRemovedFirst

PriorityQueue<EmergencyCall> ordering               equalPriorityUsesArrivalSequenceForFcfs

AmbulanceCallCenter.cadRecommendationContext()      emptySystemHasNoCadRecommendation

AmbulanceCallCenter.approveRecommendation()         dispatcherApprovesCadRecommendation

Ambulance.isAppropriateFor()                        onlyAppropriateUnitsAppearInCadCandidateSet

Ambulance.isAvailable()                             activeAndAvailableAreDifferent

Ambulance.isActiveOnDuty()                          activeAndAvailableAreDifferent

AmbulanceCallCenter.authorizeMutualAid()            mutualAidMakesNeighboringUnitEligible

AmbulanceCallCenter.registerAmbulance()             duplicateAmbulanceIdIsRejected

AmbulanceCallCenter.acknowledgeDispatch()           crewMustAcknowledgeBeforeSceneArrival

AmbulanceCallCenter.escalateCall()                  escalationPreservesImmutableOrderingIdentity

Ambulance.updatePosition()                          newerGpsReadingUpdatesTheAmbulanceLocation

Ambulance.updatePosition()                          staleGpsReadingIsRejectedWithoutChangingLocation

Ambulance.beginTransport()                          transportBeforeSceneIsRejectedWithoutMutation

DispatchRecord.recordAcknowledged()                 crewAcknowledgementIsRecorded

DispatchRecord.snapshot()                           dispatchSnapshotReflectsCurrentChronology


### Traceability Summary

```text
Requirement
        ↓
Use Case
        ↓
BDD Scenario
        ↓
Robustness Behavior
        ↓
Sequence Method
        ↓
Class / Method
        ↓
TDD Unit Test
```

The traceability connects each major dispatch requirement to the use cases, robustness behavior, sequence-diagram method calls, Java classes, and automated tests that implement and verify the required behavior.


## Spring Boot MVC Architecture Analysis

The project uses Spring Boot's MVC web architecture as the boundary between the React client and the Java application. The important point is not merely that Spring annotations are present. The MVC structure is demonstrated by **where responsibilities are placed** and by the direction in which requests travel through the system.

### Why This Is MVC

MVC separates user-interface concerns from application/domain concerns.

In this project, a dispatcher action begins in the React user interface. The request reaches a Spring MVC controller. The controller does not manipulate the `PriorityQueue`, select ambulances, change ambulance state, or create dispatch records directly. Instead, it delegates the application use case to `AmbulanceDispatchFacade`. The Facade coordinates the domain objects and supporting services, and the resulting snapshots are mapped into web responses for the view.

That separation creates the following architectural path:

```text
React View
    ↓ HTTP / JSON
Spring MVC DispatchController
    ↓ application use-case call
AmbulanceDispatchFacade
    ↓ coordination
AmbulanceCallCenter + CadRecommendationService + Domain Objects
    ↓ snapshots/results
DispatchWebMapper
    ↓ response DTO / JSON
React View
```

This is MVC because the web-facing controller receives and translates user requests, the view presents information and user actions, and the model contains the application/domain state and behavior. The project uses a separate React client rather than server-rendered Spring views, so it is not a traditional Thymeleaf-style MVC application. However, the responsibilities still map cleanly to MVC, and Spring MVC provides the backend controller/request-routing layer.

### Model

The **Model** is broader than a single Java class. It consists of the domain state, business behavior, and application data required to represent ambulance dispatch.

Important model-side classes include:

- `AmbulanceCallCenter`
- `EmergencyCall`
- `Ambulance`
- `DispatchRecommendation`
- `DispatchRecord`
- `Location`
- `Priority`
- `EmergencyCallComparator`
- `CadRecommendationService`

`AmbulanceCallCenter` owns the authoritative collections and cross-object invariants. `EmergencyCall` owns emergency-call information. `Ambulance` owns ambulance availability, suitability, assignment state, and guarded lifecycle behavior. `DispatchRecommendation` represents a proposed assignment, while `DispatchRecord` represents a committed assignment.

The model does not know how React renders a screen or how an HTTP endpoint is formatted. That separation is one of the strongest reasons the project can be described as MVC rather than a controller-centric web application.

### View

The **View** is the React frontend, including the dispatcher workspace that displays waiting emergencies, recommendations, ambulance information, active dispatches, and user controls.

The view is responsible for presentation and user interaction. It should not decide which emergency call has precedence, whether an ambulance is clinically appropriate, whether a lifecycle transition is valid, or whether a stale recommendation can be committed. Those are model/application responsibilities.

### Controller

`DispatchController` is the **Spring MVC Controller**. Its job is to receive web requests, obtain request data such as recommendation identifiers or override selections, invoke the appropriate application operation, and return a response.

Representative operations include:

```text
DispatchController.recommendNext()
DispatchController.approve(recommendationId)
DispatchController.override(recommendationId, request)
```

The controller remains intentionally thin. It does not need to know that waiting calls are stored in a `PriorityQueue`, that ambulances are indexed with a `HashMap`, or that the system uses separate recommendation and record objects. Those implementation details are behind the application boundary.

### MVC Request Flow

A recommendation request illustrates the separation clearly:

1. The Emergency Dispatcher requests a recommendation from the React view.
2. React sends an HTTP request to the backend.
3. `DispatchController.recommendNext()` receives the request.
4. The controller calls `AmbulanceDispatchFacade.recommendNext()`.
5. The Facade asks `AmbulanceCallCenter` for the current CAD recommendation context.
6. The call center identifies the next waiting emergency and eligible ambulance information from authoritative state.
7. `CadRecommendationService` evaluates the candidate information and travel estimates.
8. `AmbulanceCallCenter` stores a `DispatchRecommendation`.
9. The result is converted into a web response.
10. React displays the recommendation to the dispatcher.

Approval follows the same architectural direction. The controller receives the approval request, the Facade coordinates the application operation, and the model revalidates the ambulance before changing authoritative state.

### Spring MVC Controller vs GRASP Controller

The project contains two different ideas that both use the word **Controller**.

`DispatchController` is a **Spring MVC Controller**. It is an adapter at the web boundary. It understands HTTP requests and responses.

`AmbulanceDispatchFacade` acts as the **GRASP Controller** for the dispatch use cases. It receives application-level system operations from the web layer and coordinates the objects that perform the work.

Keeping these responsibilities separate prevents a common design problem in Spring applications: a large `@RestController` that becomes responsible for HTTP handling, business workflow, data-structure manipulation, domain mutation, and external-service coordination all at once.


## Facade Design Pattern Analysis

### Facade Intent

The **Facade** pattern provides a simplified interface to a more complex subsystem. The client communicates with the Facade rather than learning how to coordinate every class behind it.

In this project, `AmbulanceDispatchFacade` is the Facade. The client of the Facade is primarily `DispatchController`. Behind the Facade are the dispatch subsystem classes and responsibilities, including `AmbulanceCallCenter`, `CadRecommendationService`, waiting-call data structures, ambulance state, recommendations, dispatch records, and travel-estimation behavior.

The Facade exposes application-oriented operations instead of collection-oriented operations:

```text
recommendNext()
approveRecommendation(recommendationId)
overrideRecommendation(recommendationId, ambulanceId, reason)
acknowledgeDispatch(ambulanceId)
```

These operations describe **what the user is trying to accomplish**, not **how the subsystem is implemented**.

### Facade Participants

The pattern can be mapped to the project as follows:

| Facade Role | Project Class / Component | Responsibility |
|---|---|---|
| Client | `DispatchController` | Requests dispatch use cases without coordinating subsystem details. |
| Facade | `AmbulanceDispatchFacade` | Provides a small application-facing interface and coordinates use-case execution. |
| Subsystem | `AmbulanceCallCenter` | Owns authoritative state, collections, and dispatch invariants. |
| Subsystem | `CadRecommendationService` | Evaluates candidate ambulance information and recommends a response. |
| Subsystem | `EmergencyCallComparator` / `PriorityQueue` | Maintains waiting-call precedence. |
| Subsystem | `Ambulance` | Owns availability, suitability, assignment, and lifecycle behavior. |
| Subsystem | `DispatchRecommendation` | Represents a recommendation before commitment. |
| Subsystem | `DispatchRecord` | Represents a confirmed dispatch. |
| External abstraction | `TravelEstimateProvider` / `RouteProvider` | Isolates routing/travel technology from the application workflow. |

### Why the Facade Is Necessary

Without the Facade, `DispatchController` would need to know too much. A controller might otherwise have to:

1. Ask the call center for the next waiting emergency.
2. Read ambulance availability.
3. Filter candidates by capability and jurisdiction.
4. Request travel estimates.
5. invoke CAD ranking.
6. create and store a recommendation.
7. later retrieve that recommendation.
8. revalidate the selected ambulance.
9. mutate ambulance state.
10. remove or update waiting-call state.
11. create a dispatch record.
12. update active-dispatch collections and history.
13. map the result into an HTTP response.

That would create high coupling between the web layer and the dispatch subsystem. It would also make the controller difficult to test and difficult to change.

With the Facade, the controller only needs to understand the application contract:

```text
HTTP request
    ↓
Facade operation
    ↓
Application result
    ↓
HTTP response
```

The complexity still exists, because the dispatch problem is genuinely complex, but the complexity is **contained behind an intentional boundary**.

### What the Facade Should and Should Not Own

The Facade should own **application coordination**. It decides which subsystem operation should be invoked to complete a use case.

The Facade should **not** become a replacement for the domain model.

For example:

- `Ambulance.isAppropriateFor(...)` belongs on `Ambulance` because the ambulance owns the information needed to determine its suitability.
- `Ambulance.assignTo(...)` belongs on `Ambulance` because the ambulance owns its assignment state.
- `EmergencyCallComparator.compare(...)` owns queue ordering policy.
- `AmbulanceCallCenter` owns the collections and cross-object invariants.
- `CadRecommendationService` owns candidate recommendation logic.
- `DispatchController` owns HTTP request/response concerns.

The Facade coordinates these responsibilities but does not absorb them.

### Facade Request Flow

The recommendation path can be represented as:

```text
DispatchController
    ↓ recommendNext()
AmbulanceDispatchFacade
    ↓ cadRecommendationContext()
AmbulanceCallCenter
    ↓ candidate/context data
AmbulanceDispatchFacade
    ↓ recommend(context)
CadRecommendationService
    ↓ recommendation decision
AmbulanceDispatchFacade
    ↓ createRecommendation(...)
AmbulanceCallCenter
    ↓ DispatchRecommendationSnapshot
DispatchController
```

The approval path is intentionally shorter at the controller boundary:

```text
DispatchController
    ↓ approve(recommendationId)
AmbulanceDispatchFacade
    ↓ approveRecommendation(recommendationId)
AmbulanceCallCenter
    ↓ revalidate + assign + create DispatchRecord
AmbulanceDispatchFacade
    ↓ DispatchBatchSnapshot
DispatchController
```

The web layer therefore does not need to understand the internal dispatch transaction.

### Facade Benefits

The Facade provides several concrete benefits in this project:

**Lower coupling.** `DispatchController` depends on one application-facing component instead of depending directly on every dispatch subsystem class.

**Higher cohesion.** HTTP handling stays in the controller, application coordination stays in the Facade, domain rules stay in domain objects, and data-structure ownership stays in `AmbulanceCallCenter`.

**Simpler testing.** Controller tests can verify delegation and web behavior without reconstructing the entire dispatch subsystem. Facade/application tests can verify use-case coordination separately.

**Change isolation.** The implementation of CAD ranking, route estimation, or collection management can change without forcing the HTTP controller contract to change.

**Clearer use-case API.** The public methods on the Facade correspond closely to meaningful user/system operations rather than low-level object manipulation.

**Protection of invariants.** The controller cannot casually manipulate the queue, fleet map, available set, or dispatch records because those structures are not its responsibility.


## GRASP, SOLID, and Refactoring Analysis

### GRASP

The design uses several GRASP principles to assign responsibilities.

**Controller.** `AmbulanceDispatchFacade` acts as the GRASP Controller for dispatch-oriented system operations. The Spring MVC controller remains a web adapter, while the Facade coordinates the use case.

**Information Expert.** `Ambulance` determines whether it is available and appropriate because it owns its status, duty status, capability, jurisdiction, location, and current assignment. `EmergencyCall` owns priority and response requirements. `EmergencyCallComparator` owns the ordering rule needed by the waiting-call queue. `AmbulanceCallCenter` owns cross-object state because it owns the collections that must remain consistent.

**Low Coupling.** The web controller does not directly manipulate the priority queue, hash maps, ambulance lifecycle, recommendation service, or route provider. The Facade and provider interfaces reduce the number of direct dependencies between layers.

**High Cohesion.** Each major component has a focused responsibility: the controller handles HTTP, the Facade coordinates use cases, the call center protects system state, the comparator orders calls, the CAD service ranks candidates, and domain entities protect their own behavior.

**Creator.** `AmbulanceCallCenter` creates or commits recommendation/dispatch objects in the workflows where it already owns the related calls, ambulances, and collections. This keeps creation close to the information required to establish valid relationships.

**Indirection.** `AmbulanceDispatchFacade` provides indirection between the MVC controller and the domain subsystem. `TravelEstimateProvider` and `RouteProvider` provide indirection between application logic and external routing technology.

**Protected Variations.** Provider interfaces protect the core dispatch application from changes in external routing or travel-estimate technology. The rest of the application can depend on the abstraction rather than a specific vendor integration.

**Pure Fabrication.** `CadRecommendationService` is a service object introduced to keep candidate-ranking responsibility cohesive rather than forcing that behavior into an entity that does not naturally own the entire recommendation process.

### SOLID

**Single Responsibility Principle.** The design separates web handling, workflow coordination, queue ordering, candidate ranking, ambulance lifecycle behavior, and dispatch-state ownership. This is the strongest SOLID characteristic in the design.

**Open/Closed Principle.** External travel or route behavior is accessed through provider abstractions. New provider implementations can be introduced without rewriting the domain objects that consume travel information.

**Liskov Substitution Principle.** Provider implementations should be substitutable anywhere the application expects the provider interface, provided they honor the same behavioral contract. The interface boundary makes this substitutability possible.

**Interface Segregation Principle.** Focused provider interfaces such as `TravelEstimateProvider` and `RouteProvider` prevent higher-level components from depending on one oversized external-service interface containing operations they do not need.

**Dependency Inversion Principle.** Higher-level dispatch behavior depends on provider abstractions rather than depending directly on a concrete Google Routes implementation. This isolates infrastructure details from application policy.

### Refactoring

The current design reflects several important refactoring moves.

**Fat Controller → Thin Controller + Facade.** Workflow logic is moved out of `DispatchController`, reducing the number of reasons the Spring controller would need to change.

**Embedded ordering logic → `EmergencyCallComparator`.** Priority and FCFS tie-breaking are extracted into one explicit, testable ordering policy used by the priority queue.

**Recommendation = Dispatch → Separate `DispatchRecommendation` and `DispatchRecord`.** A proposed ambulance assignment is no longer treated as if it were already committed. This makes the human approval step explicit in the model.

**Direct external dependency → Provider interfaces.** Route and travel-estimate behavior are isolated behind abstractions, reducing infrastructure coupling.

**Unprotected state mutation → Aggregate-owned invariants.** `AmbulanceCallCenter` owns the authoritative collections and performs revalidation before dispatch commitment, reducing the risk of stale or inconsistent state.

**Single availability concept → Duty status + operational availability.** Separating ACTIVE/on-duty from AVAILABLE models the domain more accurately and prevents an ambulance that is staffed but already committed from being treated as dispatchable.

**Scattered lifecycle checks → Guarded ambulance behavior.** Lifecycle transitions are enforced through ambulance operations so invalid state changes are rejected close to the object that owns the state.

These refactorings improve readability, testability, maintainability, and traceability while reducing coupling and increasing cohesion.


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

A normal FIFO queue would provide `O(1)` enqueue/dequeue behavior, but it would encode the wrong business rule. FIFO means the first call received is always the first call removed. Emergency dispatch requires a later higher-priority emergency to move ahead of earlier lower-priority calls.

The priority queue solves this by defining precedence as:

```text
1. Medical Priority
2. arrivalSequence
```

Conceptually:

```text
CRITICAL, sequence 105
CRITICAL, sequence 109
HIGH,     sequence 101
MEDIUM,   sequence 103
LOW,      sequence 100
```

Even though the LOW call arrived first, it should not be dispatched before a later CRITICAL call. Among equal-priority calls, the lower `arrivalSequence` remains first.

### Why `arrivalSequence` Is Necessary

A timestamp alone is not sufficient for deterministic FCFS ordering because two calls can share the same timestamp resolution. `arrivalSequence` gives every accepted call a monotonically ordered identity for queue precedence.

The comparator can therefore preserve the business rule:

```text
Higher medical priority first
        ↓ tie
Smaller arrivalSequence first
```

This is stable from the application's perspective even though Java `PriorityQueue` itself does not promise stable ordering for elements that compare as exactly equal, because `arrivalSequence` prevents distinct queued calls from being equal under the intended precedence rule.


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
