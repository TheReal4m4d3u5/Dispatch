package edu.ics240.dispatch.bdd;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Binds the Dispatch Ambulance narrative to the system.
 *
 * <p>Note what this file does not import: no Ambulance, no AmbulanceCall, no facade, no
 * enums. Every production type is behind {@link DispatchWorld}. That is deliberate - it
 * means changing a constructor or renaming a method never touches this file, and there is
 * exactly one place to adapt the suite to your codebase.
 *
 * <p>Section headings quote the numbered step from the use case, so the mapping between
 * narrative and test stays checkable by eye.
 */
public class DispatchStepDefinitions {

    private final DispatchWorld world;

    public DispatchStepDefinitions(DispatchWorld world) {
        this.world = Objects.requireNonNull(world, "world");
    }

    // ================================================================ Background

    @Given("the following ambulances are in service:")
    public void theFollowingAmbulancesAreInService(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            world.createUnit(
                    row.get("unit"),
                    row.get("capability"),
                    row.get("jurisdiction"),
                    Double.parseDouble(row.get("latitude")),
                    Double.parseDouble(row.get("longitude")),
                    row.get("idle for"));
        }
    }

    @Given("emergency call {long} is recorded at {string} in {word}")
    public void emergencyCallIsRecorded(long callId, String address, String jurisdiction) {
        world.recordCall(callId, address, jurisdiction);
    }

    // ================================================================ Step 1
    // Emergency Dispatcher completes the emergency call evaluation and marks the
    // call ready for dispatch.

    @When("the dispatcher completes evaluation of call {long} as {word} priority requiring {word}")
    @Given("call {long} has been evaluated as {word} priority requiring {word}")
    public void completeEvaluation(long callId, String priority, String capability) {
        world.completeEvaluation(callId, priority, capability);
    }

    // ================================================================ Steps 2 and 3
    // System identifies the ambulances that are currently available and appropriate,
    // then determines which one is the best choice.

    @When("dispatcher {string} requests a recommendation")
    @Given("dispatcher {string} has been given a recommendation")
    public void dispatcherRequestsARecommendation(String dispatcherId) {
        assertTrue(world.requestRecommendation(dispatcherId),
                "dispatcher " + dispatcherId + " received no recommendation");
    }

    @Then("the system identifies {int} available and appropriate ambulances")
    public void theSystemIdentifiesCandidates(int expected) {
        assertEquals(expected, world.candidateCount());
    }

    @Then("the system recommends {string}")
    public void theSystemRecommends(String callSign) {
        assertEquals(callSign, world.recommendedUnit());
    }

    // ================================================================ Step 4
    // Emergency Dispatcher reviews the recommended ambulance.

    @Then("the recommendation shows an estimated travel time")
    public void theRecommendationShowsAnEstimate() {
        assertTrue(world.recommendedEtaSeconds() > 0,
                "there is nothing to review if the recommendation carries no rationale");
    }

    @Then("the dispatcher is offered {int} alternative ambulance(s)")
    public void theDispatcherIsOfferedAlternatives(int expected) {
        assertEquals(expected, world.alternativeCount(),
                "ALT 5A needs a list to choose from");
    }

    // ================================================================ Step 5
    // Emergency Dispatcher accepts the recommendation or chooses another
    // appropriate ambulance.

    @When("the dispatcher accepts the recommended ambulance")
    @Given("the dispatcher has accepted the recommended ambulance")
    public void theDispatcherAcceptsTheRecommendation() {
        assertTrue(world.acceptRecommendation(), "the recommended ambulance was not accepted");
    }

    /** ALT 5A. */
    @When("the dispatcher selects {string} instead, because of {word}")
    public void theDispatcherSelectsInstead(String callSign, String reason) {
        assertTrue(world.selectAlternate(callSign, reason), "the override was not accepted");
        assertEquals(reason, world.recordedOverrideReason(),
                "an override is reviewed afterwards, so the reason must be recorded");
    }

    // ================================================================ Step 6
    // System verifies that the selected ambulance is still available and that the
    // emergency call still requires dispatch.

    @When("the system verifies the selection")
    public void theSystemVerifiesTheSelection() {
        world.validateSelection();
    }

    @Then("the selection is valid")
    public void theSelectionIsValid() {
        assertEquals("OK", world.validationStatus(), world.validationMessage());
    }

    /** ALT 6A. */
    @Then("the dispatcher is told the ambulance is no longer available")
    public void toldAmbulanceNoLongerAvailable() {
        assertEquals("AMBULANCE_UNAVAILABLE", world.validationStatus(), world.validationMessage());
    }

    /** ALT 6A - "provides another appropriate ambulance for review. Return to Step 4." */
    @Then("a replacement recommendation of {string} is offered for review")
    public void aReplacementIsOffered(String callSign) {
        assertNotNull(world.replacementRecommendedUnit(),
                "ALT 6A returns to Step 4, so a fresh recommendation must come back with the refusal");
        assertEquals(callSign, world.replacementRecommendedUnit());
    }

    /** ALT 6B trigger. */
    @When("call {long} no longer requires an ambulance")
    public void callNoLongerRequiresAnAmbulance(long callId) {
        world.cancelDispatchRequirement(callId);
    }

    /** ALT 6B. */
    @Then("the dispatcher is told the call no longer requires dispatch")
    public void toldCallNoLongerRequiresDispatch() {
        assertEquals("CALL_NO_LONGER_REQUIRES_DISPATCH", world.validationStatus(),
                world.validationMessage());
    }

    // ================================================================ Step 7
    // Emergency Dispatcher confirms the dispatch.

    @When("the dispatcher confirms the dispatch")
    public void theDispatcherConfirms() {
        world.confirmDispatch();
    }

    /** ALT 7A. */
    @When("the dispatcher cancels the proposed assignment")
    public void theDispatcherCancels() {
        world.cancelProposedAssignment();
    }

    @Then("the dispatch is recorded")
    public void theDispatchIsRecorded() {
        assertTrue(world.lastDispatchSucceeded(), world.confirmMessage());
        assertEquals(1, world.activeDispatchCount());
    }

    @Then("no dispatch is recorded")
    public void noDispatchIsRecorded() {
        assertEquals(0, world.activeDispatchCount());
    }

    // ================================================================ Step 8
    // System sends the emergency assignment to the selected Ambulance Crew.

    @When("the assignment is delivered to the crew")
    public void theAssignmentIsDelivered() {
        world.deliverAssignment();
    }

    // ================================================================ Step 9
    // Ambulance Crew acknowledges the dispatch and begins responding.

    @When("the crew acknowledges the dispatch")
    public void theCrewAcknowledges() {
        assertTrue(world.acknowledgeDispatch(), "no such dispatch to acknowledge");
    }

    @When("the crew begins responding")
    public void theCrewBeginsResponding() {
        assertTrue(world.beginResponse(), "no such dispatch to respond to");
    }

    @Then("the dispatch status is {word}")
    public void theDispatchStatusIs(String status) {
        assertEquals(status, world.dispatchStatus());
    }

    // ================================================================ shared assertions

    @Then("call {long} is waiting for dispatch")
    public void callIsWaitingForDispatch(long callId) {
        assertTrue(world.isWaitingForDispatch(callId),
                "call " + callId + " should still be in the waiting queue");
    }

    @Then("call {long} is no longer waiting for dispatch")
    public void callIsNoLongerWaiting(long callId) {
        assertFalse(world.isWaitingForDispatch(callId),
                "call " + callId + " should have left the waiting queue");
    }

    @When("{string} goes out of service for {word}")
    public void goesOutOfService(String callSign, String status) {
        world.takeOutOfService(callSign, status);
    }

    @Then("{string} is {word}")
    public void unitIs(String callSign, String status) {
        assertEquals(status, world.statusOf(callSign));
    }
}
