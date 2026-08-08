Ambulance Call Center and Dispatch System

ICS 240 Data Structures, MVC, GRASP, SOLID, Facade, and ICONIX-Based Design

This project is a Java 21, Spring Boot 3.5.4, React 19, and Vite 6 ambulance call-center and Computer-Aided Dispatch (CAD) system built as an ICS 240 Data Structures case study.

The project combines a realistic, human-supervised ambulance dispatch workflow with explicit data-structure design, object-oriented responsibility assignment, MVC architecture, GRASP, SOLID, the Facade pattern, UML, and ICONIX-style use-case-driven design.

The current production waiting-call structure is:

PriorityQueue<EmergencyCall> waitingCalls =
    new PriorityQueue<>(new EmergencyCallComparator());

There is no production custom MinHeap.java. Java's PriorityQueue<EmergencyCall> is the authoritative production priority queue.

Current Revision

The current revision reflects the latest domain-model, use-case, robustness, sequence-diagram, MVC, GRASP, and source-level method review.

Major current decisions:

PriorityQueue<EmergencyCall> stores waiting calls.

EmergencyCallComparator provides medical-priority and stable FCFS precedence.

HashMap, HashSet, ArrayList, EnumMap, and EnumSet support other access patterns.

ACTIVE/on-duty and AVAILABLE are separate ambulance concepts.

CAD recommends the closest appropriate available Ambulance.

The Emergency Dispatcher reviews, approves, or overrides the recommendation.

DispatchRecommendation represents a proposed assignment.

DispatchRecord is the concrete Java class representing the committed Dispatch.

The Ambulance Crew acknowledges a committed dispatch in a separate crew use case.

AmbulanceDispatchFacade is the application-level GRASP Controller.

Spring MVC Controllers remain thin HTTP-boundary objects.

AmbulanceCallCenter is the aggregate root and owns cross-object consistency.

CadRecommendationService ranks immutable CAD candidate context rather than owning live domain state.

TravelEstimateProvider and RouteProvider isolate external routing behavior.

GoogleRoutesClient implements the routing provider abstractions.

Sequence diagrams use whole-number method-call labels only.

Return arrows are shown only when the returned value adds design value.

loop, alt, guards, states, returned values, and creation notes are not numbered as method calls.

Current production Java source inventory:

Java type

Count

Classes

29

Records

50

Enums

14

Interfaces

4

Total

97

Table of Contents

Project Purpose

Current System Behavior

Realistic Dispatch Workflow

ICONIX Modeling Approach

Domain Model

Use-Case Packages

ED-01 Dispatch Ambulance

ED-01 Robustness Analysis

ED-01 Sequence Design

MVC Architecture

GRASP Design

Facade Design Pattern

SOLID Design

Data Structures

Queue Ordering

Big-O Analysis

CAD Ambulance Selection

Ambulance Lifecycle

Dispatch Acknowledgement

Actors and Views

REST and Integration Boundaries

Testing and Verification

Running the Project

Project Structure

Design Documentation

Current Verification Status

Project Purpose

The project has two connected goals.

The first goal is to model the behavior of an ambulance call center and dispatch operation:

EmergencyCall intake
        ↓
medical evaluation
        ↓
Priority assignment
        ↓
waiting-call ordering
        ↓
eligible Ambulance discovery
        ↓
CAD recommendation
        ↓
Emergency Dispatcher review
        ↓
dispatch confirmation
        ↓
Ambulance Crew acknowledgement
        ↓
response lifecycle

The second goal is to make the data-structure and object-oriented design decisions explicit.

The project therefore asks:

Which EmergencyCall should be handled first?

What happens when two EmergencyCalls have the same Priority?

How is first-come, first-served ordering preserved?

Which Ambulances are actually AVAILABLE?

Which available Ambulances are clinically appropriate?

Which appropriate Ambulance can provide the best response?

Who is allowed to confirm the Dispatch?

Which object owns lifecycle transitions?

Which object owns waiting-call ordering?

Which object creates DispatchRecommendation and DispatchRecord objects?

Which layer may mutate shared domain state?

How are external routing providers isolated?

How are concurrent Spring requests prevented from corrupting state?

Current System Behavior

The system follows a human-supervised CAD model.

ACTIVE / On Duty Is Not the Same as AVAILABLE

An Ambulance may be:

DutyStatus.ACTIVE

while still being unavailable because it is:

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

A dispatch candidate must satisfy the relevant rules, including:

ACTIVE duty status
AVAILABLE operational status
required BLS/ALS capability
jurisdiction eligibility
mutual-aid authorization when required
valid Location

The system recommends an Ambulance, but the human Emergency Dispatcher remains responsible for the final operational decision.

Realistic Dispatch Workflow

The main business workflow is:

EmergencyCall is evaluated
        ↓
EmergencyCall receives Priority and response requirements
        ↓
EmergencyCall waits for dispatch
        ↓
System identifies appropriate available Ambulances
        ↓
System evaluates travel/proximity
        ↓
System creates a DispatchRecommendation
        ↓
Emergency Dispatcher reviews recommendation
        ↓
Emergency Dispatcher approves or overrides
        ↓
System commits a Dispatch
        ↓
DispatchRecord is created
        ↓
Ambulance becomes DISPATCHED

Crew acknowledgement is modeled separately:

Dispatch committed
        ↓
Ambulance Crew receives assignment
        ↓
Crew acknowledges
        ↓
DispatchRecord records acknowledgement
        ↓
Ambulance transitions DISPATCHED → EN_ROUTE

If no appropriate Ambulance is available, the EmergencyCall remains waiting.

Possible operational responses include:

wait for an appropriate resource
request mutual aid
request additional EMS
request Fire response
request Police response
request specialized emergency support
escalate the shortage

ICONIX Modeling Approach

The project uses a use-case-driven design process consistent with the ICONIX material used for the course work.

The analysis/design progression is:

identify real-world domain objects
        ↓
draw/refine domain model
        ↓
identify actors and use cases
        ↓
organize use cases into packages
        ↓
write first-draft use cases
        ↓
robustness analysis
        ↓
sequence diagrams
        ↓
detailed class design
        ↓
implementation and tests

Use-Case Writing Rules

Formal use-case text follows these rules:

Use active voice.

Identify who performs every action.

Write both sides of the actor/system dialogue.

Use present tense.

Use agreed domain names consistently.

Describe observable business/system behavior.

Do not put PriorityQueue, HashMap, Controllers, Facades, Java methods, REST, or Google Routes into first-draft use-case behavior.

Keep GUI implementation details out unless the interface detail is behaviorally important.

Include the basic course and meaningful alternate courses.

Write enough detail to drive robustness and sequence diagrams later.

Example:

Emergency Dispatcher reviews the recommended Ambulance.

The system verifies that the selected Ambulance remains eligible.

Emergency Dispatcher confirms the Dispatch.

The system records the Dispatch.

Not:

The PriorityQueue polls a call.
The Controller calls the Facade.
The HashMap retrieves an Ambulance.

Those belong to later software design.

Domain Model

The first-pass business-domain model remains centered on six major concepts:

EmergencyCall
Ambulance
Dispatch
Location
Priority
Hospital

EmergencyCall

Represents an emergency incident requiring evaluation and possibly an EMS response.

Important information includes:

Priority
Location
medical determinant
response plan
required ClinicalCapability
jurisdiction
mutual-aid authorization
received time
arrivalSequence

Ambulance

Represents an EMS response resource.

Important information includes:

DutyStatus
AmbulanceStatus
ClinicalCapability
Location
current assignment
availableSince
GPS/AVL position
state history

Dispatch

Represents the assignment of an Ambulance to an EmergencyCall.

The current concrete Java implementation represents this concept with:

DispatchRecord

DispatchRecord owns the committed assignment chronology and response milestones.

Location

Represents geographic position and owns direct geographic-distance behavior.

Priority

Represents the urgency of an EmergencyCall.

Current levels are:

CRITICAL
HIGH
MEDIUM
LOW
NON_EMERGENCY

Hospital

Represents the patient transport destination.

Hospital remains a valid business-domain concept, but there is currently no dedicated Hospital Java class. Hospital destinations are represented with Location values in the current implementation.

Hospital does not participate in ED-01 Dispatch Ambulance because the transport destination is not yet known during initial dispatch.

Use-Case Packages

Use cases are grouped by functional area so diagrams remain readable and the actor is not presented with an unbounded flat list.

1. Emergency Intake

Primary actor: Emergency Dispatcher

Accept Emergency Call
Evaluate Emergency Call
Assign Priority
Change Priority
Determine Response Needs
Cancel Emergency Response

2. Dispatch

Primary actor: Emergency Dispatcher

View Waiting Emergencies
Dispatch Ambulance
Review Ambulance Recommendation
Override Ambulance Recommendation
View Available Ambulances
Monitor Active Dispatches
Handle Unacknowledged Dispatch
Reassign Emergency Resource

3. Resource Coordination

Primary actor: Emergency Dispatcher

Request Emergency Resource
Request Ambulance Response
Request Fire Response
Request Police Response
Request Additional EMS
Authorize Mutual Aid
Request Specialized Response
Request Emergency Support

Request Emergency Resource may be treated as the generalized use case for more specific response requests.

4. Field Response

Primary actor: Ambulance Crew

View Assigned Emergency
Acknowledge Dispatch
View Route to Emergency
Update Ambulance Location
Report Arrival on Scene
Manage On-Scene Response
Manage Patient Transport
Complete Emergency Response
Return Ambulance to Service

5. Fleet Operations

Primary actor: Fleet Supervisor

View Ambulance Fleet
Register Ambulance
Manage Ambulance Availability
Manage Refueling
Manage Ambulance Maintenance
Mark Ambulance Unstaffed
Take Ambulance Out of Service
Restore Ambulance to Service
Review Ambulance Status History

6. Administration

Primary actor: Administrator

Review System Statistics
Review Dispatch History
Review Ambulance Utilization
Review Emergency Call Activity
Review Operational Performance

Packages organize the use-case model. They do not imply that each package is itself a giant <<include>> use case.

ED-01 Dispatch Ambulance

Basic Course

The business-level use case is intentionally implementation-neutral.

Step

Event / Response

1

The Emergency Dispatcher completes the EmergencyCall evaluation and indicates that the EmergencyCall is ready for dispatch.

2

The system identifies Ambulances that are currently available and appropriate for the EmergencyCall.

3

The system determines which appropriate Ambulance can provide the best response and presents that Ambulance as the recommendation.

4

The Emergency Dispatcher reviews the recommended Ambulance.

5

The Emergency Dispatcher accepts the recommended Ambulance or selects another appropriate Ambulance.

6

The system verifies that the selected Ambulance is still eligible and that the EmergencyCall still requires a response.

7

The Emergency Dispatcher confirms the Dispatch.

8

The system records the Dispatch between the EmergencyCall and selected Ambulance.

Crew acknowledgement is handled in the separate Ambulance Crew use case.

Important Alternate Courses

ALT — No appropriate Ambulance
    EmergencyCall remains waiting.

ALT — Dispatcher chooses another Ambulance
    System validates the alternate Ambulance before commitment.

ALT — Selected Ambulance is no longer eligible
    System rejects the stale selection and presents another option.

ALT — EmergencyCall no longer requires Dispatch
    System does not commit an assignment.

ALT — Dispatcher does not confirm
    System does not create a DispatchRecord.

ED-01 Robustness Analysis

The robustness diagram bridges the business use case and the detailed design.

General interaction rule:

Actor
  ↓
Boundary
  ↓
Control
  ↓
Entity

Boundary

For ED-01 the conceptual boundary is:

DispatchWorkspace

It represents the dispatcher-facing UI/workspace.

Primary Robustness Behaviors and Entities

Robustness behavior

Primary entity

Supporting entities

Identify Ambulances

Ambulance

EmergencyCall, Priority, Location

Determine Best Choice

Ambulance

EmergencyCall, Location, Priority

Review Recommended Ambulance

Ambulance

EmergencyCall, DispatchRecommendation

Dispatcher Accepts or Chooses Another

Ambulance

EmergencyCall, DispatchRecommendation

Revalidate Selected Ambulance

Ambulance

EmergencyCall, DispatchRecommendation

Confirm Dispatch

Dispatch / DispatchRecord

EmergencyCall, Ambulance

Record Confirmed Assignment

DispatchRecord

EmergencyCall, Ambulance

The primary entity changes during the use case:

SELECTION PHASE
    primary entity = Ambulance

        ↓ confirmation

COMMITTED DISPATCH PHASE
    primary entity = Dispatch / DispatchRecord

Information-Expert Interpretation

Ambulance is the primary entity for selection because it owns:

availability
duty status
capability
jurisdiction
Location
current assignment

EmergencyCall supplies:

Priority
incident Location
required ClinicalCapability
jurisdiction
mutual-aid permission

Location owns distance calculations.

DispatchRecommendation represents the proposed assignment.

DispatchRecord represents the committed assignment after approval.

Hospital is not used in ED-01.

ED-01 Sequence Design

The sequence diagram is a detailed design artifact, not the same thing as the use-case text or robustness diagram.

Participant Roles

The detailed design may include:

Emergency Dispatcher          actor

DispatchWorkspace             <<boundary>>

DispatchController            MVC web controller / boundary adapter

AmbulanceDispatchFacade       <<control>> / GRASP Controller

CadRecommendationService      application service / control

TravelEstimateProvider        external-service abstraction

AmbulanceCallCenter           aggregate root / model

EmergencyCall                 <<entity>>

Ambulance                     <<entity>>

DispatchRecommendation        <<entity>>

DispatchRecord                <<entity>>

Sequence-Diagram Numbering Rule

Only actual calls are numbered.

Use:

1
2
3
4
5
...

Do not use:

1.1
1.2
4.3
5.4.1

Do not number:

return values
context
snapshots returned on dashed arrows
loop labels
alt labels
guards
states
notes

A loop is shown inside a UML combined-fragment box, just like alt.

Example:

loop [for each candidate Ambulance]

    8. isAvailable()
    9. isActiveOnDuty()
   10. isAppropriateFor(...)
   11. snapshot()

end

An alt is also boxed:

alt [Ambulance still eligible]

    assignTo(...)
    create DispatchRecord

else [Ambulance no longer eligible]

    reject approval

end

Return arrows are dashed and are shown only when the returned value is used later in a meaningful way.

Verified Public Operations for ED-01

Web / MVC

DispatchController:

recommendNext()
approve(recommendationId)
override(recommendationId, request)

DispatchWebMapper:

toRecommendationResponse(snapshot)
toDispatchBatchResponse(snapshot)

Application / GRASP Controller

AmbulanceDispatchFacade:

recommendNext()
approveRecommendation(recommendationId)
overrideRecommendation(recommendationId, ambulanceId, reason)

CAD Ranking

CadRecommendationService:

recommend(context)
estimate(origin, destination)        private

TravelEstimateProvider:

computeTravelEstimate(origin, destination)

Aggregate Root

AmbulanceCallCenter:

cadRecommendationContext()
createRecommendation(callId, ambulanceId, estimate)
approveRecommendation(recommendationId)
overrideRecommendation(recommendationId, ambulanceId, reason)

requireRecommendation(recommendationId)        private
recommendationSnapshot(recommendation)         private
findWaitingCall(callId)                        private
requireAmbulance(ambulanceId)                  private
dispatchSpecific(...)                          private
dispatchBatch(record)                          private

EmergencyCall

getCurrentPriority()
getLocation()
getRequiredCapability()
getJurisdiction()
isMutualAidAllowed()

Ambulance

isAvailable()
isActiveOnDuty()
isAppropriateFor(...)
getLocation()
snapshot()
assignTo(call, at)

DispatchRecommendation

recommendationId()
callId()
recommendedAmbulanceId()
travelEstimate()
createdAt()

DispatchRecommendation does not own a snapshot() method.

DispatchRecord

snapshot()

DispatchRecord also owns chronology operations used later in the response lifecycle.

Source-Aligned ED-01 Call Flow

At a useful design level, the recommendation/approval flow is:

DispatchController.recommendNext()
        ↓
AmbulanceDispatchFacade.recommendNext()
        ↓
AmbulanceCallCenter.cadRecommendationContext()
        ↓
CadRecommendationService.recommend(context)
        ↓
TravelEstimateProvider.computeTravelEstimate(...)
        ↓
AmbulanceCallCenter.createRecommendation(...)
        ↓
AmbulanceCallCenter.recommendationSnapshot(...)
        ↓
DispatchWebMapper.toRecommendationResponse(...)
        ↓

Emergency Dispatcher reviews recommendation

        ↓
DispatchController.approve(recommendationId)
        ↓
AmbulanceDispatchFacade.approveRecommendation(recommendationId)
        ↓
AmbulanceCallCenter.approveRecommendation(recommendationId)
        ↓
requireRecommendation(...)
        ↓
findWaitingCall(...)
        ↓
requireAmbulance(...)
        ↓
dispatchSpecific(...)
        ↓
Ambulance.isAppropriateFor(...)
        ↓
Ambulance.assignTo(call, dispatchedAt)
        ↓
DispatchRecord is constructed
        ↓
dispatchBatch(record)
        ↓
DispatchRecord.snapshot()
        ↓
DispatchWebMapper.toDispatchBatchResponse(...)

The sequence diagram should not number returned values such as:

CadRecommendationContextSnapshot
CadRecommendationDecision
DispatchRecommendationSnapshot
DispatchBatchSnapshot
DispatchSnapshot
TravelEstimate

If those values are shown as dashed returns, the return arrows remain unnumbered.

MVC Architecture

The system follows MVC with a separate application boundary.

VIEW
React role-specific workspaces
        ↓ HTTP

CONTROLLER
Spring MVC Controllers
        ↓

APPLICATION / GRASP CONTROLLER
AmbulanceDispatchFacade
        ↓

MODEL
AmbulanceCallCenter
CadRecommendationService
RouteService
Domain Entities
Policies
Snapshots
Data Structures
        ↓

INFRASTRUCTURE
GoogleRoutesClient

View

The React View is responsible for:

presenting system state
collecting user actions
role-specific presentation
sending REST requests
displaying results/errors
map and route presentation

It does not own dispatch priority, lifecycle rules, or queue mechanics.

MVC Controller

Operational controllers include:

CallController
AmbulanceController
DispatchController
SystemController

An MVC Controller is responsible for:

receive HTTP request
        ↓
validate transport data
        ↓
call Facade
        ↓
map result
        ↓
return HTTP response

MVC Controller vs GRASP Controller

These are related but not identical concepts.

DispatchController
    = Spring MVC Controller / HTTP boundary

AmbulanceDispatchFacade
    = GRASP Controller / application-use-case coordinator

The name Controller on a Spring class does not automatically make that class the GRASP Controller for domain behavior.

GRASP Design

Information Expert

EmergencyCall

Expert on:

Priority
incident Location
required ClinicalCapability
jurisdiction
mutual-aid permission
arrival order

Ambulance

Expert on:

availability
duty state
capability
jurisdiction
Location
current assignment
legal lifecycle transitions

DispatchRecord

Expert on:

committed Dispatch chronology
acknowledgement
scene arrival
transport
hospital arrival
completion
queue wait
service duration
override audit data

Location

Expert on geographic coordinates and direct-distance calculations.

EmergencyCallComparator

Expert on waiting-call precedence.

Creator

AmbulanceCallCenter is the primary Creator for objects it aggregates and records, including:

EmergencyCall
Ambulance
DispatchRecommendation
DispatchRecord

This keeps creation close to the object that owns the relevant collections and invariants.

Controller

Primary GRASP Controller:

AmbulanceDispatchFacade

Low Coupling

Key dependency directions:

DispatchController
    ↓
AmbulanceDispatchFacade

AmbulanceDispatchFacade
    ↓
AmbulanceCallCenter
CadRecommendationService
RouteService

CadRecommendationService
    ↓
TravelEstimateProvider

RouteService
    ↓
RouteProvider

High Cohesion

Responsibilities are intentionally separated:

EmergencyCall                call state
Ambulance                    resource/lifecycle state
EmergencyCallComparator      precedence
DispatchRecord               dispatch chronology
CadRecommendationService     ranking
RouteService                 route query
AmbulanceCallCenter          aggregate consistency
AmbulanceDispatchFacade      use-case coordination
DispatchWebMapper            DTO mapping

Polymorphism / Protected Variations

Variation is isolated through focused interfaces:

CadResponsePolicy
EmergencyMedicalEvaluationPolicy
TravelEstimateProvider
RouteProvider

Pure Fabrication / Indirection

Examples:

AmbulanceDispatchFacade
CadRecommendationService
RouteService
SystemStatisticsAccumulator
DispatchWebMapper
ApiExceptionHandler

Facade Design Pattern

AmbulanceDispatchFacade gives Spring MVC Controllers a simplified application boundary.

Controller
    ↓
AmbulanceDispatchFacade
    ↓
┌─────────────────────────────┐
│ AmbulanceCallCenter         │
│ CadRecommendationService    │
│ RouteService                │
└─────────────────────────────┘

The Facade owns:

application-use-case coordination
synchronization
snapshot boundaries
coordination across aggregate and application services

The Facade does not own:

HTTP
JSON
React rendering
PriorityQueue ordering
Ambulance lifecycle rules
Dispatch chronology
Google-specific behavior

MVC and Facade therefore complement each other.

SOLID Design

Single Responsibility Principle

Examples:

EmergencyCall               emergency-call state
Ambulance                   resource/lifecycle state
DispatchRecord              dispatch chronology
EmergencyCallComparator     queue precedence
CadRecommendationService    candidate ranking
RouteService                route queries
DispatchWebMapper           DTO mapping

Open/Closed Principle

Variation can be extended behind:

CadResponsePolicy
EmergencyMedicalEvaluationPolicy
TravelEstimateProvider
RouteProvider

Liskov Substitution Principle

Implementations of those interfaces can substitute without forcing callers to know the concrete type.

Interface Segregation Principle

The project uses small, focused provider/policy interfaces instead of one large service interface.

Dependency Inversion Principle

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

Data Structures

Waiting Calls

PriorityQueue<EmergencyCall>

Purpose:

retrieve the highest-precedence waiting EmergencyCall

Fleet Lookup

HashMap<Integer, Ambulance>

Purpose:

direct Ambulance lookup by ID

Available Ambulance Index

HashSet<Integer>

Purpose:

index units that may immediately accept a call

Ambulance.isAvailable() remains the authoritative business rule.

Active Dispatch Index

HashMap<Integer, DispatchRecord>

Purpose:

find the active DispatchRecord for an Ambulance
enforce no-double-dispatch invariant

Recommendation Index

HashMap<Long, DispatchRecommendation>

Purpose:

track CAD recommendations awaiting dispatcher action

Dispatch History

ArrayList<DispatchRecord>

Purpose:

append and review completed dispatch history

Statistics

Enum-oriented statistics use structures such as:

EnumMap
EnumSet

Queue Ordering

EmergencyCallComparator defines the ordering used by the waiting-call PriorityQueue.

The precedence is:

1. current Priority rank
2. arrivalSequence

Therefore:

CRITICAL
    before
HIGH
    before
MEDIUM
    before
LOW
    before
NON_EMERGENCY

For equal Priority:

smaller arrivalSequence
    =
earlier arrival
    =
served first

This provides deterministic FCFS behavior.

Why Arrival Sequence Is Required

Two calls can share:

same Priority
same timestamp

A monotonically increasing arrivalSequence provides an unambiguous stable tie-break.

Java PriorityQueue Iteration Rule

Java does not guarantee that iteration over a PriorityQueue is sorted.

Use:

peek()
poll()

for priority behavior.

For a fully ordered UI list:

copy queue
    ↓
sort copy with EmergencyCallComparator

Do not assume for-each iteration returns priority order.

Big-O Analysis

Let:

n = waiting calls
a = available candidate ambulances
h = dispatch history size

Operation

Main structure

Complexity

Add waiting call

PriorityQueue

O(log n)

View next waiting call

peek()

O(1)

Remove next waiting call

poll()

O(log n)

Remove specific waiting call

remove(Object)

O(n)

Build completely sorted waiting list

copy + sort

O(n log n)

Ambulance lookup by ID

HashMap

expected O(1)

Available-ID membership

HashSet

expected O(1)

Active-dispatch lookup

HashMap

expected O(1)

Recommendation lookup

HashMap

expected O(1)

Append history

ArrayList

amortized O(1)

Evaluate eligible candidates

candidate scan

approximately O(a) before route-provider cost

Priority escalation requires removal and reinsertion because the comparator key changes.

CAD Ambulance Selection

The system makes two separate decisions:

Which EmergencyCall goes first?
        ↓
PriorityQueue + EmergencyCallComparator

Which Ambulance is best for that EmergencyCall?
        ↓
eligibility filtering + CadRecommendationService

Do not merge these into one permanent heap.

Candidate Eligibility

Eligibility considers:

ACTIVE
AVAILABLE
required capability
jurisdiction
mutual aid
current Location

Ranking

CadRecommendationService receives an immutable:

CadRecommendationContextSnapshot

and produces a:

CadRecommendationDecision

The ranking may use a TravelEstimateProvider.

External route work occurs outside authoritative domain mutation.

The aggregate later creates the authoritative:

DispatchRecommendation

The recommendation is revalidated before commitment.

Ambulance Lifecycle

The operational lifecycle includes:

AVAILABLE
    ↓
DISPATCHED
    ↓
EN_ROUTE
    ↓
ON_SCENE
    ↓
TRANSPORTING
    ↓
AT_HOSPITAL
    ↓
RETURNING_TO_SERVICE
    ↓
CLEANING_AND_RESTOCKING
    ↓
AVAILABLE

No-transport and operational-removal paths are also supported.

Examples:

ON_SCENE
    ↓
complete without transport
    ↓
RETURNING_TO_SERVICE

and:

AVAILABLE
    ↓
REFUELING / MAINTENANCE / UNSTAFFED / OUT_OF_SERVICE
    ↓
not dispatchable

Ambulance owns legal lifecycle transitions.

Dispatch Acknowledgement

Crew acknowledgement is deliberately separated from ED-01.

ED-01 ends when the dispatcher successfully commits the assignment.

The Ambulance Crew use case then begins:

Crew receives assignment
        ↓
acknowledges Dispatch
        ↓
DispatchRecord records acknowledgedAt
        ↓
Ambulance acknowledges dispatch
        ↓
DISPATCHED → EN_ROUTE

Relevant current operations include:

AmbulanceController.acknowledgeDispatch(id)

AmbulanceDispatchFacade.acknowledgeDispatch(ambulanceId)

AmbulanceCallCenter.acknowledgeDispatch(ambulanceId)

DispatchRecord.canRecordAcknowledgement(at)

DispatchRecord.recordAcknowledged(at)

Ambulance.acknowledgeDispatch(at)

Actors and Views

Emergency Dispatcher

Primary responsibilities include:

emergency intake/evaluation
manage waiting emergencies
request/review CAD recommendation
approve or override recommendation
coordinate emergency resources
monitor active responses
manage shortages

Ambulance Crew

Primary responsibilities include:

view assigned emergency
acknowledge dispatch
view route
update location
report arrival
manage on-scene response
transport patient
complete response
return to service

Fleet Supervisor

Primary responsibilities include:

view fleet
register Ambulance
manage availability
refueling
maintenance
unstaffed state
out-of-service state
restore service
review status history

Administrator

Primary responsibilities include:

review statistics
review dispatch history
review utilization
review call activity
review operational performance

REST and Integration Boundaries

The frontend communicates with Spring MVC Controllers over HTTP.

Operational Controllers do not directly manipulate:

PriorityQueue
HashMap
HashSet
AmbulanceCallCenter internals
CadRecommendationService internals
RouteService internals
GoogleRoutesClient

The dependency direction is:

React
    ↓ HTTP
Spring MVC Controller
    ↓
AmbulanceDispatchFacade
    ↓
Model / Application Services
    ↓
Domain / Data Structures

External routing is isolated behind:

TravelEstimateProvider
RouteProvider

with GoogleRoutesClient as a concrete provider implementation.

Testing and Verification

The project includes multiple levels of verification:

JUnit 5
Cucumber BDD
REST/API testing
dependency-free core self-test
console CAD demonstration
GRASP/SOLID architecture-boundary checks
frontend/mobile foundations

Testing should cover:

PriorityQueue precedence
equal-priority FCFS ordering
duplicate IDs
ambulance eligibility
double-dispatch prevention
recommendation revalidation
ambulance lifecycle transitions
dispatch chronology
route-provider fallback
invalid operations
concurrency invariants

Running the Project

Backend

Typical Spring Boot execution:

mvn spring-boot:run

Tests:

mvn test

Frontend

cd frontend
npm install
npm run dev

Production frontend build:

npm run build

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
│   └── test/
│
├── frontend/
├── mobile-tests/
├── api-tests/
├── docs/
│   └── uml/
├── scripts/
├── pom.xml
└── README.md

Design Documentation

Important supporting design artifacts include:

docs/MVC_ROLE_VIEWS.md
docs/REAL_WORLD_CAD_WORKFLOW.md
docs/GRASP_SOLID_ANALYSIS_AND_CHANGES.md
docs/CLASS_ATTRIBUTES_METHODS_RESPONSIBILITIES.md
docs/UML_CLASSES_ATTRIBUTES_METHODS.md
docs/USE_CASE_DESIGN_STEPS.md
docs/FULL_TRACEABILITY_MATRIX.md
docs/REST_API_TESTING.md
docs/REFACTORING_HISTORY.md
docs/uml/

Current ED-01 design artifacts should distinguish:

Use Case
    business actor/system dialogue

Robustness Diagram
    boundary/control/entity responsibilities

Sequence Diagram
    ordered method calls and object collaboration

Class Diagram
    static classes, fields, methods, relationships

The project uses UML/ICONIX modeling guidance from the course reference materials, including use-case-driven object modeling and standard UML sequence-diagram notation.

Current Verification Status

The current README is aligned with the latest project design review.

Previously verified project checks:

Check

Current result

GRASP/SOLID boundary check

PASS

Dependency-free core self-test

PASS — 65 checks

Console CAD demonstration

PASS

Production waiting queue

java.util.PriorityQueue<EmergencyCall>

Production custom MinHeap.java

Absent

Production Java types

97

Spring/Maven full suite

Requires normal Maven/dependency environment

Frontend build

Requires normal Node dependency environment

Important enforced architectural rules include:

web Controllers do not directly manipulate AmbulanceCallCenter internals

web Controllers do not directly coordinate CadRecommendationService

CadRecommendationService does not depend back on AmbulanceDispatchFacade

RouteService depends on RouteProvider

CadRecommendationService depends on TravelEstimateProvider

core does not depend on web/DTO packages

AmbulanceCallCenter owns PriorityQueue<EmergencyCall>

production MinHeap.java is absent

Final Architecture Summary

Emergency Dispatcher / Ambulance Crew / Fleet Supervisor / Administrator
                                ↓
                         React Views
                                ↓
                    Spring MVC Controllers
                                ↓
                    AmbulanceDispatchFacade
                         /            \
                        /              \
         CadRecommendationService     RouteService
                    ↓                    ↓
         TravelEstimateProvider      RouteProvider
                    \                   /
                     \                 /
                       GoogleRoutesClient

                    AmbulanceDispatchFacade
                                ↓
                     AmbulanceCallCenter
                                ↓
      ┌─────────────────────────────────────────────┐
      │ PriorityQueue<EmergencyCall>                │
      │ HashMap<Integer, Ambulance>                 │
      │ HashSet<Integer>                            │
      │ HashMap<Integer, DispatchRecord>            │
      │ HashMap<Long, DispatchRecommendation>       │
      │ ArrayList<DispatchRecord>                   │
      │ EnumMap / EnumSet                           │
      └─────────────────────────────────────────────┘
                                ↓
     EmergencyCall / Ambulance / DispatchRecord / Location / Priority

The central design rule is:

Let each domain entity own the behavior for which it has the necessary information, let the aggregate own cross-object invariants and data structures, let the Facade coordinate application use cases, and let the MVC Controller remain a thin HTTP boundary.

For ED-01 specifically:

Ambulance
    = primary entity during resource selection

Dispatch / DispatchRecord
    = primary entity after dispatch confirmation

EmergencyCall / Priority / Location
    = supporting information experts

Hospital
    = not part of initial dispatch