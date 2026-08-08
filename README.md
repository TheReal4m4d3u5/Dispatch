# Ambulance Call Center and Dispatch System

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-green.svg)
![React](https://img.shields.io/badge/React-19-blue.svg)

## Description

The Ambulance Call Center and Dispatch System is a Java-based emergency medical dispatch application that manages emergency calls, ambulance availability, CAD recommendations, dispatch assignments, ambulance response states, and operational history.

The application was designed using an object-oriented approach that focuses on modeling the main concepts in the ambulance dispatch domain as software objects. The design centers around AmbulanceCallCenter, which manages waiting EmergencyCall objects, Ambulance resources, DispatchRecommendation objects, DispatchRecord objects, and the data structures used to maintain the system state.

The project uses a Java PriorityQueue<EmergencyCall> to maintain waiting emergency calls. EmergencyCallComparator orders calls by medical Priority first and arrivalSequence second. This allows a later CRITICAL call to move ahead of a lower-priority call while still preserving first-come, first-served ordering when two calls have the same Priority.

The dispatch workflow is human supervised. The system identifies eligible Ambulances and creates a recommendation, but the Emergency Dispatcher reviews the recommendation and either approves it or selects another appropriate Ambulance. The system revalidates the selected Ambulance before committing the Dispatch so that a stale recommendation cannot cause an invalid or duplicate assignment.

The project also uses Spring Boot and React to separate the user interface, web controllers, application coordination, and domain behavior. AmbulanceDispatchFacade coordinates application use cases, while AmbulanceCallCenter owns the authoritative in-memory data structures and dispatch invariants.

The project includes JUnit unit tests, Cucumber BDD tests, REST/API tests, and architecture checks. JUnit verifies lower-level class and data-structure behavior, while Cucumber verifies use-case behavior such as stable call ordering, CAD recommendation, ambulance lifecycle rules, and dispatcher approval.


## Problem

An emergency dispatch center needs a consistent way to determine which EmergencyCall should be handled first and which Ambulance should respond.

A simple first-come, first-served queue is not sufficient because a later CRITICAL EmergencyCall may need to be handled before an earlier LOW-priority EmergencyCall. At the same time, equal-priority calls still need to remain first come, first served. Two calls may also have the same Priority and the same timestamp, so the system needs another deterministic ordering value.

The system must also distinguish between an Ambulance that is active/on duty and an Ambulance that is actually available. An Ambulance may be staffed and active but unavailable because it is already DISPATCHED, EN_ROUTE, ON_SCENE, TRANSPORTING, AT_HOSPITAL, REFUELING, in MAINTENANCE, or otherwise unable to accept another call.

After selecting the next EmergencyCall, the system must determine which Ambulances are available and appropriate for that emergency. Clinical capability, jurisdiction, mutual-aid authorization, current Location, and travel time may all affect the recommendation. The Emergency Dispatcher must still be able to review and override the recommendation before the assignment becomes a confirmed Dispatch.

The proposed system solves these problems by combining a stable PriorityQueue for waiting EmergencyCalls with a human-supervised CAD recommendation and dispatch workflow.


## Table of Contents

- [Ambulance Call Center and Dispatch System](#ambulance-call-center-and-dispatch-system)
  - [Description](#description)
  - [Problem](#problem)
  - [Table of Contents](#table-of-contents)
  - [Design Process](#design-process)
  - [Assumptions and Open Questions](#assumptions-and-open-questions)
  - [Design Decision Log](#design-decision-log)
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
