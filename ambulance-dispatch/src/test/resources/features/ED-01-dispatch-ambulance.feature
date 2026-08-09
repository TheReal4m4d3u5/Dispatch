@ED-01
Feature: Dispatch Ambulance
  As an Emergency Dispatcher
  I want to dispatch an available and appropriate ambulance to an emergency
  So that a crew is responding to the patient
 
  Each scenario below is one column of the use case. Step comments give the
  numbered step from the narrative so the mapping stays checkable.
 
  Background:
    Given the following ambulances are in service:
      | unit    | capability | jurisdiction | latitude | longitude | idle for   |
      | MEDIC-1 | ALS        | NORTH        | 44.9800  | -93.2700  | 30 minutes |
      | MEDIC-2 | ALS        | NORTH        | 45.2000  | -93.6000  | 90 minutes |
    And emergency call 1001 is recorded at "410 Nicollet Mall" in NORTH
 
 
  Scenario: Main flow - the dispatcher accepts the recommended ambulance
 
    # 1. Emergency Dispatcher completes the emergency call evaluation
    #    and marks the call ready for dispatch.
    When the dispatcher completes evaluation of call 1001 as HIGH priority requiring BLS
    Then call 1001 is waiting for dispatch
 
    # 2. System identifies the ambulances that are currently available
    #    and appropriate for the emergency.
    When dispatcher "A" requests a recommendation
    Then the system identifies 2 available and appropriate ambulances
 
    # 3. System determines which available ambulance is the best choice.
    And the system recommends "MEDIC-1"
 
    # 4. Emergency Dispatcher reviews the recommended ambulance.
    And the recommendation shows an estimated travel time
    And the dispatcher is offered 1 alternative ambulance
 
    # 5. Emergency Dispatcher accepts the recommendation.
    When the dispatcher accepts the recommended ambulance
 
    # 6. System verifies that the selected ambulance is still available
    #    and that the emergency call still requires dispatch.
    And the system verifies the selection
    Then the selection is valid
 
    # 7. Emergency Dispatcher confirms the dispatch.
    When the dispatcher confirms the dispatch
    Then the dispatch is recorded
    And "MEDIC-1" is DISPATCHED
    And call 1001 is no longer waiting for dispatch
 
    # 8. System sends the emergency assignment to the selected Ambulance Crew.
    When the assignment is delivered to the crew
    Then the dispatch status is ASSIGNED
 
    # 9. Ambulance Crew acknowledges the dispatch and begins responding.
    When the crew acknowledges the dispatch
    Then the dispatch status is ACKNOWLEDGED
    When the crew begins responding
    Then the dispatch status is IN_PROGRESS
    And "MEDIC-1" is EN_ROUTE
 
 
  @ALT-5A
  Scenario: ALT 5A - Dispatcher Selects Another Ambulance
    Given call 1001 has been evaluated as HIGH priority requiring BLS
    And dispatcher "A" has been given a recommendation
 
    # 5. Emergency Dispatcher rejects the recommendation and selects
    #    another appropriate ambulance.
    When the dispatcher selects "MEDIC-2" instead, because of LOCAL_KNOWLEDGE
 
    # 6. System verifies that the selected ambulance is still available
    #    and that the emergency call still requires a response.
    And the system verifies the selection
    Then the selection is valid
 
    # 7. Emergency Dispatcher confirms the dispatch.
    When the dispatcher confirms the dispatch
    Then the dispatch is recorded
    And "MEDIC-2" is DISPATCHED
    And "MEDIC-1" is AVAILABLE
 
 
  @ALT-6A
  Scenario: ALT 6A - Ambulance No Longer Available
    Given call 1001 has been evaluated as HIGH priority requiring BLS
    And dispatcher "A" has been given a recommendation
    And the dispatcher has accepted the recommended ambulance
 
    When "MEDIC-1" goes out of service for MAINTENANCE
    And the system verifies the selection
 
    # System informs the Emergency Dispatcher that the selected ambulance
    # is no longer available and provides another appropriate ambulance
    # for review. Return to Step 4.
    Then the dispatcher is told the ambulance is no longer available
    And a replacement recommendation of "MEDIC-2" is offered for review
    And call 1001 is waiting for dispatch
 
 
  @ALT-6B
  Scenario: ALT 6B - Call No Longer Requires Dispatch
    Given call 1001 has been evaluated as HIGH priority requiring BLS
    And dispatcher "A" has been given a recommendation
    And the dispatcher has accepted the recommended ambulance
 
    When call 1001 no longer requires an ambulance
    And the system verifies the selection
 
    # System informs the Emergency Dispatcher that the emergency call no
    # longer requires an ambulance response and ends the dispatch process.
    Then the dispatcher is told the call no longer requires dispatch
    And no dispatch is recorded
    And "MEDIC-1" is AVAILABLE
 
 
  @ALT-7A
  Scenario: ALT 7A - Dispatcher Does Not Confirm
    Given call 1001 has been evaluated as HIGH priority requiring BLS
    And dispatcher "A" has been given a recommendation
    And the dispatcher has accepted the recommended ambulance
 
    # Emergency Dispatcher cancels the proposed assignment before dispatch.
    When the dispatcher cancels the proposed assignment
 
    # System does not record the assignment and keeps the emergency call
    # available for further dispatch action.
    Then no dispatch is recorded
    And "MEDIC-1" is AVAILABLE
    And call 1001 is waiting for dispatch