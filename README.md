Ambulance Call Center and Dispatch System

ICS 240 Data Structures, MVC, GRASP, SOLID, and Realistic CAD Dispatch

This project is a Java 21, Spring Boot, and React ambulance call-center and dispatch system designed as an ICS 240 Data Structures case study.

The system models a realistic Computer-Aided Dispatch (CAD) workflow while keeping the course's data-structure and object-oriented design goals visible.

The current design demonstrates:

java.util.PriorityQueue<EmergencyCall>

stable emergency-call precedence

HashMap, HashSet, ArrayList, EnumMap, and EnumSet

Big-O analysis

object-oriented domain modeling

MVC architecture

GRASP responsibility assignment

SOLID principles

the Facade design pattern

human-supervised CAD recommendation

BLS/ALS capability matching

ambulance availability and lifecycle management

GPS/AVL position updates

Google Routes integration with a local fallback

Spring REST APIs

React role-specific Views

JUnit, Cucumber BDD, REST testing, and dependency-free core verification

Table of Contents

Project Purpose

Current System Behavior

Realistic Dispatch Workflow

MVC Architecture

Facade Design Pattern

GRASP Design

SOLID Design

Core Domain Model

Data Structures

Queue Ordering

Big-O Analysis

Ambulance Selection

Ambulance Status Model

Dispatch Acknowledgement

Primary Actors and Views

REST API

GPS and Google Routes

Testing and Verification

Running the Backend

Running the Frontend

Environment Variables

Project Structure

Design Documentation

Current Verification Status

Project Purpose

The project has two related goals.

The first goal is to model the behavior of an ambulance call center and dispatch operation. Emergency calls are evaluated, assigned a medical priority, placed into a waiting structure, matched with appropriate available ambulances, reviewed by an Emergency Dispatcher, dispatched, acknowledged by the Ambulance Crew, and tracked through the response lifecycle.

The second goal is to make the data structures and object-oriented design decisions explicit.

The project therefore treats the following as first-class design concerns:

Which EmergencyCall should be handled first?

Which Ambulances can respond?

Which available Ambulance is the closest appropriate resource?

What happens when two calls have the same Priority?

What happens when an Ambulance becomes unavailable?

Who owns lifecycle transitions?

Who owns the waiting-call PriorityQueue?

Which layer is allowed to manipulate domain state?

How are external routing services isolated from the domain?

How are concurrent web requests prevented from corrupting shared state?

Current System Behavior

The current system follows a human-supervised CAD model.

It does not treat every on-duty ambulance as automatically dispatchable.

It distinguishes:

ACTIVE / ON DUTY
        ≠
AVAILABLE

An ambulance can be active but unavailable because it is:

DISPATCHED
EN_ROUTE
ON_SCENE
TRANSPORTING
AT_HOSPITAL
RETURNING_TO_SERVICE
CLEANING_AND_RESTOCKING
REFUELING

It may also be operationally unavailable because it is:

MAINTENANCE
UNSTAFFED
OUT_OF_SERVICE

The system recommends the closest appropriate available ambulance, but the Emergency Dispatcher supervises the decision.

The dispatcher may:

approve the CAD recommendation
or
override the recommendation with another eligible ambulance

The Ambulance Crew then acknowledges the dispatch and begins the response.

Realistic Dispatch Workflow

The primary dispatch workflow is:

911 emergency information received
        ↓
Emergency Dispatcher completes call evaluation
        ↓
EmergencyCall receives medical determinant and Priority
        ↓
EmergencyCall becomes ready for dispatch
        ↓
System identifies appropriate available Ambulances
        ↓
System evaluates proximity / travel estimates
        ↓
System creates a CAD recommendation
        ↓
Emergency Dispatcher reviews recommendation
        ↓
Emergency Dispatcher approves or overrides
        ↓
System creates the Dispatch
        ↓
Assignment is sent to Ambulance Crew
        ↓
Ambulance Crew acknowledges Dispatch
        ↓
Ambulance becomes EN_ROUTE

If no appropriate ambulance is currently available, the call remains waiting.

Possible operational responses include:

wait for an appropriate unit to become available
authorize mutual aid
use another permitted responder
reposition resources
escalate the shortage

The current classroom implementation models the core ambulance/mutual-aid behavior without attempting to reproduce every agency-specific policy.

MVC Architecture

The application follows MVC at the system level.

VIEW
React role-specific workspaces
        ↓ HTTP

CONTROLLER
Spring MVC REST Controllers
        ↓

APPLICATION BOUNDARY
AmbulanceDispatchFacade
        ↓

MODEL
Application Services
AmbulanceCallCenter
Domain Entities
Policies
Data Structures
        ↓

INFRASTRUCTURE
GoogleRoutesClient

View

The React frontend is responsible for:

displaying system state
collecting user input
role-specific presentation
sending REST requests
displaying results and errors

The View does not own dispatch priority rules, ambulance lifecycle rules, or queue mechanics.

Controller

Spring MVC Controllers are responsible for:

receiving HTTP requests
validating transport input
calling the Facade
mapping results to response DTOs
returning HTTP responses

Controllers do not directly manipulate:

PriorityQueue
HashMap
HashSet
AmbulanceCallCenter
CadRecommendationService
RouteService
GoogleRoutesClient

Model

The Model contains:

AmbulanceDispatchFacade
CadRecommendationService
RouteService
AmbulanceCallCenter
EmergencyCall
Ambulance
DispatchRecord
Location
Priority
domain policies
snapshots
data structures

Facade Design Pattern

AmbulanceDispatchFacade is the main application boundary.

It provides Controllers with one simplified entry point into the Model.

Controller
    ↓
AmbulanceDispatchFacade
    ↓
┌────────────────────────────────────┐
│ AmbulanceCallCenter                │
│ CadRecommendationService           │
│ RouteService                       │
└────────────────────────────────────┘

The Facade is responsible for:

application use-case coordination

synchronization of shared in-memory state

obtaining immutable snapshots

coordinating CAD recommendation

coordinating route requests

preventing Controllers from reaching into domain internals

The Facade does not own:

HTTP

React presentation

JSON mapping

PriorityQueue ordering

Ambulance lifecycle rules

Dispatch chronology

Google-specific HTTP behavior

MVC and Facade are therefore complementary.

MVC organizes the application.

Facade simplifies access from the Controller to the Model subsystem.

GRASP Design

The current responsibility assignments follow GRASP.

Information Expert

EmergencyCall

Owns emergency-call information such as:

Priority
medical determinant
Location
response requirements
arrival sequence
mutual-aid authorization

Ambulance

Owns:

availability
duty status
clinical capability
Location
GPS position
assigned call
operational lifecycle

DispatchRecord

Owns:

dispatch chronology
acknowledgement
scene arrival
transport
hospital arrival
completion
override audit information

Location

Owns validated geographic coordinates and local geographic-distance calculation.

Creator

AmbulanceCallCenter creates and registers the domain objects that it aggregates and coordinates.

Examples:

AmbulanceCallCenter → EmergencyCall
AmbulanceCallCenter → Ambulance
AmbulanceCallCenter → DispatchRecommendation
AmbulanceCallCenter → DispatchRecord

Controller

The GRASP Controller is:

AmbulanceDispatchFacade

Spring MVC Controllers are transport boundaries.

Low Coupling

Important coupling rules include:

Controller → Facade
CadRecommendationService → TravelEstimateProvider
RouteService → RouteProvider
AmbulanceCallCenter → CadResponsePolicy
AmbulanceCallCenter → EmergencyMedicalEvaluationPolicy

High Cohesion

Each major class has a focused responsibility.

AmbulanceCallCenter is intentionally the aggregate root because cross-object dispatch mutations must remain consistent.

Polymorphism

Variation is represented through focused interfaces:

CadResponsePolicy
EmergencyMedicalEvaluationPolicy
TravelEstimateProvider
RouteProvider

Pure Fabrication

Examples include:

AmbulanceDispatchFacade
CadRecommendationService
RouteService
SystemStatisticsAccumulator
DispatchWebMapper
ApiExceptionHandler

Indirection and Protected Variations

External routing, response policy, medical evaluation policy, HTTP representation, and concurrency are isolated behind stable boundaries.

SOLID Design

Single Responsibility Principle

Examples:

EmergencyCall
    emergency-call state

Ambulance
    ambulance availability and lifecycle

DispatchRecord
    dispatch chronology

EmergencyCallComparator
    waiting-call precedence

CadRecommendationService
    candidate ranking

RouteService
    route-query behavior

DispatchWebMapper
    domain/snapshot ↔ DTO mapping

Open/Closed Principle

Important behavior can be extended through:

CadResponsePolicy
EmergencyMedicalEvaluationPolicy
TravelEstimateProvider
RouteProvider

without rewriting the calling classes.

Liskov Substitution Principle

Implementations may substitute behind those interfaces without requiring callers to know the concrete implementation.

Interface Segregation Principle

The project uses small focused interfaces rather than one large general-purpose service interface.

Dependency Inversion Principle

High-level services depend on abstractions.

CadRecommendationService
    ↓
TravelEstimateProvider
    ↑
GoogleRoutesClient

RouteService
    ↓
RouteProvider
    ↑
GoogleRoutesClient

Core Domain Model

The major business-domain concepts are:

EmergencyCall
Ambulance
Dispatch
Location
Priority
Hospital

These are intentionally different from implementation concepts.

For example:

PriorityQueue
HashMap
Facade
Controller
DTO
GoogleRoutesClient

are software-design or implementation elements rather than initial business-domain entities.

EmergencyCall

Represents the emergency incident.

Important concepts associated with a call include:

medical evaluation
Priority
Location
response requirements
arrival order
mutual aid

Ambulance

Represents an EMS response resource.

Important concepts include:

ACTIVE vs INACTIVE
AVAILABLE vs unavailable
BLS vs ALS capability
Location
GPS position
current assignment
response status

Dispatch

Represents the assignment of an Ambulance to an EmergencyCall.

A Dispatch tracks response milestones and dispatcher decision information.

Location

Represents geographic position.

Priority

Current priority values are:

CRITICAL
HIGH
MEDIUM
LOW
NON_EMERGENCY

Smaller numeric ranks mean more urgent calls.

Hospital

Hospital becomes relevant when a response requires patient transport.

Data Structures

The project intentionally uses standard Java data structures for different access patterns.

Waiting Calls

PriorityQueue<EmergencyCall> waitingCalls =
        new PriorityQueue<>(new EmergencyCallComparator());

Purpose:

retrieve the highest-precedence waiting EmergencyCall efficiently

Fleet Lookup

HashMap<Integer, Ambulance> fleetById

Purpose:

direct lookup of an Ambulance by ID

Available Ambulance Index

HashSet<Integer> availableAmbulanceIds

Purpose:

track units that can immediately accept a new incident

The Ambulance entity remains authoritative for actual eligibility.

Active Dispatch Index

HashMap<Integer, DispatchRecord> activeDispatchesByAmbulanceId

Purpose:

find the active Dispatch for an Ambulance

Recommendation Index

HashMap<Long, DispatchRecommendation> recommendationsById

Purpose:

track CAD recommendations awaiting dispatcher action

Dispatch History

ArrayList<DispatchRecord> dispatchHistory

Purpose:

append completed Dispatch records
support history/reporting

Statistics

SystemStatisticsAccumulator uses enum-oriented structures such as EnumMap.

Purpose:

maintain statistics without repeatedly rescanning all domain objects

Queue Ordering

EmergencyCallComparator defines the ordering of:

PriorityQueue<EmergencyCall>

The ordering key is:

1. current Priority rank
2. arrivalSequence

Therefore:

CRITICAL before HIGH
HIGH before MEDIUM
MEDIUM before LOW
LOW before NON_EMERGENCY

If two calls have the same Priority:

earlier arrivalSequence wins

This provides deterministic FCFS behavior for equal-priority calls.

Why Arrival Sequence Is Used

Wall-clock timestamps are not sufficient by themselves.

Two calls may have:

same Priority
same timestamp

The monotonically increasing arrival sequence creates a unique ordering.

Example:

Call A: HIGH, sequence 15
Call B: HIGH, sequence 16

The queue returns Call A first.

Important PriorityQueue Rule

Java does not guarantee sorted iterator order for a PriorityQueue.

Therefore:

peek() / poll()

are used for queue precedence.

When the UI needs the entire waiting list in sorted order, the application creates a defensive copy and sorts that copy with EmergencyCallComparator.

Big-O Analysis

Let:

n = number of waiting calls
a = number of available ambulances
f = number of fleet ambulances
h = number of historical dispatches

Operation

Main Structure

Time Complexity

Add waiting call

PriorityQueue

O(log n)

View next waiting call

PriorityQueue.peek()

O(1)

Remove next waiting call

PriorityQueue.poll()

O(log n)

Remove a specific queued call

PriorityQueue.remove(Object)

O(n)

Produce complete sorted waiting list

copy + sort

O(n log n)

Ambulance lookup by ID

HashMap

expected O(1)

Available-ID membership

HashSet

expected O(1)

Active-dispatch lookup by ambulance

HashMap

expected O(1)

Recommendation lookup by ID

HashMap

expected O(1)

Append completed dispatch

ArrayList

amortized O(1)

Inspect all eligible ambulance candidates

available set + lookups

approximately O(a) before external route cost

Priority escalation is intentionally more expensive than peek() because changing a call's ordering key requires removal and reinsertion.

Conceptually:

find/remove specific waiting call  O(n)
reinsert updated call              O(log n)

Overall:

O(n)

because the linear removal dominates.

Ambulance Selection

The CAD process does not select merely the closest ambulance.

It selects the closest appropriate available resource.

A candidate must satisfy rules such as:

on duty / ACTIVE
currently AVAILABLE
required clinical capability
jurisdiction rules
mutual-aid rules
valid location information

Then eligible candidates are compared using travel/proximity information.

The current design supports road-route estimates through TravelEstimateProvider.

Deterministic ranking uses:

1. travel duration
2. travel distance
3. availableSince
4. ambulance ID

This ensures that candidate selection is reproducible even when travel estimates tie.

The CAD result is a recommendation.

The Emergency Dispatcher remains responsible for supervising the assignment.

Ambulance Status Model

The current lifecycle states are:

AVAILABLE
DISPATCHED
EN_ROUTE
ON_SCENE
TRANSPORTING
AT_HOSPITAL
RETURNING_TO_SERVICE
CLEANING_AND_RESTOCKING
REFUELING
MAINTENANCE
UNSTAFFED
OUT_OF_SERVICE

Transport Response

AVAILABLE
    ↓ dispatch
DISPATCHED
    ↓ crew acknowledgement
EN_ROUTE
    ↓ arrive
ON_SCENE
    ↓ begin transport
TRANSPORTING
    ↓ arrive at hospital
AT_HOSPITAL
    ↓ complete hospital activity
CLEANING_AND_RESTOCKING
    ↓ ready
AVAILABLE

No-Transport Response

AVAILABLE
    ↓
DISPATCHED
    ↓
EN_ROUTE
    ↓
ON_SCENE
    ↓ no transport
RETURNING_TO_SERVICE
    ↓ ready
AVAILABLE

Refueling

AVAILABLE
    ↓
REFUELING
    ↓
AVAILABLE

This demonstrates why:

ACTIVE ≠ AVAILABLE

An ambulance may still be active/on duty while temporarily unable to accept another call.

Dispatch Acknowledgement

Dispatcher confirmation and Ambulance Crew acknowledgement are separate events.

Dispatcher

The Emergency Dispatcher:

reviews the CAD recommendation
approves or overrides it
confirms the Dispatch

Ambulance Crew

The Ambulance Crew:

receives the assignment
acknowledges the Dispatch
begins responding

The acknowledgement transition is:

DISPATCHED
    ↓ acknowledge
EN_ROUTE

If a crew does not acknowledge within the configured classroom threshold, the system can produce an operational alert for dispatcher attention.

Primary Actors and Views

The frontend uses four primary role-specific Views.

Emergency Dispatcher

Responsibilities include:

emergency-call evaluation
call intake
waiting-call monitoring
CAD recommendation review
dispatch approval
dispatch override
active-dispatch monitoring
operational statistics

Ambulance Crew / Field Crew

Responsibilities include:

view assignment
acknowledge Dispatch
view route
update position
arrive on scene
begin transport
arrive at hospital
complete response
return to service

Fleet Supervisor

Responsibilities include:

register Ambulance
view fleet
manage availability
manage maintenance
manage unstaffed/out-of-service states
restore units to operational service

Administrator

Responsibilities include:

review statistics
review history
review utilization / operational information

REST API

Backend default:

http://localhost:8080

Emergency Calls

Method

Endpoint

Purpose

POST

/api/calls

Accept/evaluate a new emergency call

GET

/api/calls/next

View the next waiting call

PATCH

/api/calls/{callId}/priority

Change/escalate call priority

PATCH

/api/calls/{callId}/mutual-aid

Change mutual-aid authorization

CAD / Dispatch

Method

Endpoint

Purpose

POST

/api/dispatch/recommendations/next

Generate the next CAD recommendation

POST

/api/dispatch/recommendations/{recommendationId}/approve

Approve recommended ambulance

POST

/api/dispatch/recommendations/{recommendationId}/override

Override recommendation with another eligible ambulance

GET

/api/dispatch/{dispatchId}/route

Get route information

GET

/api/dispatch/active

View active dispatches

Ambulances

Method

Endpoint

Purpose

POST

/api/ambulances

Register ambulance

GET

/api/ambulances

View fleet

PUT

/api/ambulances/{id}/position

Update GPS/AVL position

POST

/api/ambulances/{id}/acknowledge-dispatch

Crew acknowledges dispatch

POST

/api/ambulances/{id}/arrive-on-scene

Mark arrival on scene

POST

/api/ambulances/{id}/begin-transport

Begin patient transport

POST

/api/ambulances/{id}/arrive-at-hospital

Mark hospital arrival

POST

/api/ambulances/{id}/complete-at-hospital

Complete hospital response

POST

/api/ambulances/{id}/complete-without-transport

Complete response without transport

POST

/api/ambulances/{id}/finish-return-to-service

Finish no-transport return

POST

/api/ambulances/{id}/finish-cleaning

Finish cleaning/restocking

POST

/api/ambulances/{id}/begin-refueling

Begin refueling

POST

/api/ambulances/{id}/finish-refueling

Finish refueling

POST

/api/ambulances/{id}/maintenance

Send unit to maintenance

POST

/api/ambulances/{id}/unstaffed

Mark unit unstaffed

POST

/api/ambulances/{id}/out-of-service

Take unit out of service

POST

/api/ambulances/{id}/restore-service

Restore operational service

System

Method

Endpoint

Purpose

GET

/api/state

View current system board

GET

/api/statistics

View statistics

GET

/api/history

View dispatch history

GET

/api/ambulances/{id}/state-history

View ambulance state history

Demo

Method

Endpoint

Purpose

POST

/api/demo/load

Load demo data

POST

/api/demo/reset

Reset demo state

Demo operations are development/classroom support behavior and are not part of the central dispatch domain.

GPS and Google Routes

The system supports timestamped ambulance position information.

A position update includes geographic data used by:

fleet monitoring
CAD recommendation
crew routing
stale-position detection

Stale GPS readings are rejected so an older update cannot overwrite a newer authoritative position.

Backend Google Routes Key

The backend uses:

GOOGLE_ROUTES_API_KEY

When the Google Routes key is unavailable, the architecture supports a local geographic fallback.

Frontend Google Maps Key

The React frontend uses:

VITE_GOOGLE_MAPS_API_KEY

The frontend map is a supporting visualization.

The core dispatch system does not depend on the map UI.

Testing and Verification

The repository includes several testing layers.

Dependency-Free Core Self-Test

Run:

bash scripts/run-core-self-test.sh

Current result after updating the source list for the refactored services:

Core self-test passed: 65 checks.

The self-test exercises core behavior without requiring Spring Boot or frontend dependencies.

GRASP / SOLID Boundary Check

Run:

bash scripts/check-grasp-solid-boundaries.sh

Current result:

GRASP/SOLID boundary check: PASS

The check verifies architectural rules such as:

Controllers do not import AmbulanceCallCenter.
Controllers do not import CadRecommendationService.
Controllers do not import RouteService.
core does not import web/DTO code.
application services do not import GoogleRoutesClient directly.
CadRecommendationService does not depend on AmbulanceDispatchFacade.
AmbulanceCallCenter uses PriorityQueue<EmergencyCall>.
No production MinHeap.java exists.

JUnit

JUnit test sources cover:

domain entities
queue ordering
dispatch behavior
lifecycle behavior
REST/application behavior
negative cases

Cucumber BDD

BDD scenarios model important behavior from the user/system perspective.

REST API Testing

Manual HTTP request collections are located under:

api-tests/

Mobile Testing

The project contains a mobile-test foundation under:

mobile-tests/

Running the Backend

Requirements:

Java 21
Maven

From the project root:

mvn spring-boot:run

Backend:

http://localhost:8080

Run the full Maven test suite with:

mvn test

Running the Frontend

Requirements:

Node.js
npm

From:

frontend/

run:

npm install
npm run dev

To create a production frontend build:

npm run build

Environment Variables

Backend

GOOGLE_ROUTES_API_KEY=

Spring configuration:

google.routes.api-key=${GOOGLE_ROUTES_API_KEY:}

Frontend

Create:

frontend/.env.local

with:

VITE_GOOGLE_MAPS_API_KEY=

Example environment files are included in the frontend directory.

Do not commit production API keys.

Project Structure

ambulance-dispatch-system/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── edu/ics240/dispatch/
│   │   │       ├── config/
│   │   │       ├── console/
│   │   │       ├── core/
│   │   │       ├── demo/
│   │   │       ├── dto/
│   │   │       ├── integration/
│   │   │       ├── service/
│   │   │       └── web/
│   │   └── resources/
│   │
│   └── test/
│
├── frontend/
│   ├── public/
│   └── src/
│
├── mobile-tests/
│
├── api-tests/
│
├── docs/
│   └── uml/
│
├── scripts/
│   ├── check-grasp-solid-boundaries.sh
│   ├── run-console-demo.sh
│   └── run-core-self-test.sh
│
├── pom.xml
└── README.md

Design Documentation

The repository includes detailed supporting design documentation.

MVC

docs/MVC_ROLE_VIEWS.md

Describes role-specific Views and MVC responsibility boundaries.

Real-World CAD Workflow

docs/REAL_WORLD_CAD_WORKFLOW.md

Documents the realistic dispatch model, including:

active vs available
BLS/ALS capability
CAD recommendation
dispatcher supervision
GPS/route proximity
mutual aid
crew acknowledgement

GRASP and SOLID

docs/GRASP_SOLID_ANALYSIS_AND_CHANGES.md

Contains the detailed GRASP/SOLID review and the refactorings made to improve dependency direction.

Class Responsibilities

docs/CLASS_ATTRIBUTES_METHODS_RESPONSIBILITIES.md

Documents production classes/types, their state, methods, and responsibilities.

UML Classes — Attributes and Methods

docs/UML_CLASSES_ATTRIBUTES_METHODS.md

Contains the streamlined UML-style class listing requested for the project.

Use Cases

docs/USE_CASE_DESIGN_STEPS.md

Contains use-case design steps.

Traceability

docs/FULL_TRACEABILITY_MATRIX.md

Connects requirements, use cases, design artifacts, implementation, and tests.

REST Testing

docs/REST_API_TESTING.md

Documents REST testing strategy and scenarios.

Refactoring History

docs/REFACTORING_HISTORY.md

Records major design changes and their rationale.

Current Verification Status

The updated project was checked after the current MVC/GRASP/SOLID and PriorityQueue<EmergencyCall> refactoring.

Verified:

GRASP/SOLID boundary check: PASS
Core self-test: PASS — 65 checks
Production waiting queue: java.util.PriorityQueue<EmergencyCall>
Production custom MinHeap.java: absent
Controller → Facade boundary: enforced
CadRecommendationService → Facade dependency: absent
RouteService → GoogleRoutesClient dependency: absent

The complete Spring Boot test suite still requires Maven and normal dependency resolution:

mvn test

The frontend dependency build should be verified in a normal Node environment with:

cd frontend
npm install
npm run build

Final Architecture Summary

Emergency Dispatcher / Ambulance Crew / Fleet Supervisor / Administrator
                                ↓
                         React View
                                ↓
                    Spring MVC Controller
                                ↓
                    AmbulanceDispatchFacade
                       /              \
                      /                \
      CadRecommendationService       RouteService
                ↓                        ↓
     TravelEstimateProvider          RouteProvider
                \                       /
                 \                     /
                   GoogleRoutesClient

                    AmbulanceDispatchFacade
                                ↓
                     AmbulanceCallCenter
                                ↓
      ┌─────────────────────────────────────────┐
      │ PriorityQueue<EmergencyCall>            │
      │ HashMap<Integer, Ambulance>             │
      │ HashSet<Integer>                        │
      │ HashMap<Integer, DispatchRecord>        │
      │ HashMap<Long, DispatchRecommendation>   │
      │ ArrayList<DispatchRecord>               │
      │ EnumMap / EnumSet                       │
      └─────────────────────────────────────────┘
                                ↓
          EmergencyCall / Ambulance / DispatchRecord

The central responsibility rule is:

EmergencyCall owns emergency-call information, Ambulance owns availability and lifecycle, DispatchRecord owns dispatch chronology, EmergencyCallComparator owns waiting-call precedence, AmbulanceCallCenter owns the data structures and cross-object invariants, AmbulanceDispatchFacade owns application coordination and synchronization, specialized services own CAD ranking and route queries, Spring MVC Controllers own HTTP translation, and React owns presentation.