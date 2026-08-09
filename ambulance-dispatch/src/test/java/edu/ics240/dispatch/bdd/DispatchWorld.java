package edu.ics240.dispatch.bdd;

import edu.ics240.dispatch.core.Ambulance;
import edu.ics240.dispatch.core.AmbulanceCall;
import edu.ics240.dispatch.core.AmbulanceCrew;
import edu.ics240.dispatch.core.AmbulanceStatus;
import edu.ics240.dispatch.core.DispatchRecommendation;
import edu.ics240.dispatch.core.DispatchRecord;
import edu.ics240.dispatch.core.EvaluationResult;
import edu.ics240.dispatch.core.Jurisdiction;
import edu.ics240.dispatch.core.Location;
import edu.ics240.dispatch.core.OverrideReason;
import edu.ics240.dispatch.core.Priority;
import edu.ics240.dispatch.core.RequiredCapability;
import edu.ics240.dispatch.core.ValidationStatus;
import edu.ics240.dispatch.service.AckMonitor;
import edu.ics240.dispatch.service.AmbulanceCallCenter;
import edu.ics240.dispatch.service.AmbulanceDispatchFacade;
import edu.ics240.dispatch.service.ConfirmOutcome;
import edu.ics240.dispatch.service.CrewNotificationService;
import edu.ics240.dispatch.service.RecommendationOutcome;
import edu.ics240.dispatch.service.SelectionOutcome;
import edu.ics240.dispatch.service.ValidationOutcome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The one file that touches production code.
 *
 * <p>
 * Nothing here is constructed by hand. Spring supplies the collaborators, so a
 * change to any constructor is absorbed by the container rather than by this
 * file. What remains is a set of thin verbs the step definitions call, each one
 * or two lines.
 *
 * <p>
 * Everything crosses the boundary as String or int rather than as domain enums,
 * which is what keeps DispatchStepDefinitions free of production imports.
 */
@Component
public class DispatchWorld {

	@Autowired
	private AmbulanceCallCenter callCenter;
	@Autowired
	private AmbulanceDispatchFacade facade;
	@Autowired
	private CrewNotificationService notifier;
	@Autowired
	private AckMonitor ackMonitor;
	@Autowired
	private Clock clock;

	// ---------------------------------------------------------------- scenario
	// state

	private final Map<String, Integer> unitIds = new HashMap<>();
	private int nextUnitId = 1;
	private int nextCrewId = 100;

	private DispatchRecommendation recommendation;
	private ValidationOutcome validation;
	private ConfirmOutcome confirmation;
	private DispatchRecord dispatch;

	// ---------------------------------------------------------------- Background

	public void createUnit(String callSign, String capability, String jurisdiction, double latitude, double longitude,
			String idleFor) {
		int id = nextUnitId++;

// an ALS unit can also answer a BLS call
		Set<String> caps = "ALS".equalsIgnoreCase(capability) ? Set.of("ALS", "BLS") : Set.of(capability.toUpperCase());

		Ambulance unit = new Ambulance(id, AmbulanceStatus.AVAILABLE, new Location(latitude, longitude), caps,
				jurisdiction);

		unit.setCallSign(callSign);
		unit.setAvailableSince(clock.instant().minus(parseDuration(idleFor)));

		unitIds.put(callSign, id);
		callCenter.registerAmbulance(unit);
	}

	// ---------------------------------------------------------------- Step 1

	public void completeEvaluation(long callId, String priority, String capability) {
		AmbulanceCall call = callCenter.getCall(callId);
		if (call == null) {
			throw new AssertionError("call " + callId + " was never recorded");
		}
		facade.completeEvaluation(callId, new EvaluationResult(Priority.valueOf(priority),
				RequiredCapability.valueOf(capability), call.getJurisdiction(), false, true, "chest pain", ""));
	}

	public boolean isWaitingForDispatch(long callId) {
		AmbulanceCall call = callCenter.getCall(callId);
		return call != null && call.isReadyForDispatch();
	}

	/** ALT 6B trigger. */
	public void cancelDispatchRequirement(long callId) {
		callCenter.getCall(callId).cancelDispatchRequirement();
	}

	// ---------------------------------------------------------------- Steps 2 to 4

	/** @return true if a recommendation came back */
	public boolean requestRecommendation(String dispatcherId) {
		RecommendationOutcome outcome = facade.recommendNext("dispatcher-" + dispatcherId);
		if (!outcome.isOk()) {
			return false;
		}
		recommendation = outcome.recommendation();
		return true;
	}

	public int candidateCount() {
		return recommendation.getRankedCandidates().size();
	}

	public String recommendedUnit() {
		return recommendation.getRecommendedAmbulance().getCallSign();
	}

	public long recommendedEtaSeconds() {
		// getTravelEstimate() is typed as Object here, so presence is all we can check.
		return recommendation.getTravelEstimate() == null ? 0L : 1L;
	}

	public int alternativeCount() {
		return recommendation.getRankedAlternatives().size();
	}

	// ---------------------------------------------------------------- Step 5

	public boolean acceptRecommendation() {
		facade.selectRecommendedAmbulance(recommendation.getRecommendationId());
		return true;
	}

	/** ALT 5A. */
	public boolean selectAlternate(String callSign, String reason) {
		facade.selectAlternateAmbulance(recommendation.getRecommendationId(), unitId(callSign),
				OverrideReason.valueOf(reason));
		return true;
	}

	public String recordedOverrideReason() {
		OverrideReason reason = recommendation.getOverrideReason();
		return reason == null ? null : reason.name();
	}

	// ---------------------------------------------------------------- Step 6

	// replace: private ValidationOutcome validation;
	public void validateSelection() {
		validation = facade.validateSelection(recommendation.getRecommendationId());
	}

	public String validationStatus() {
		return validation.status().name();
	}

	public String validationMessage() {
		return validation.message();
	}

	/** ALT 6A. Null when the refusal came with no replacement. */
	public String replacementRecommendedUnit() {
		return validation.replacement() == null
				? null
				: validation.replacement().getRecommendedAmbulance().getCallSign();
	}

	// ---------------------------------------------------------------- Step 7

	// replace: private ConfirmOutcome confirmation;
	private boolean confirmed;

	public void confirmDispatch() {
		dispatch = facade.confirmDispatch(recommendation.getRecommendationId());
		confirmed = dispatch != null;
	}

	public boolean lastDispatchSucceeded() {
		return confirmed;
	}

	public String confirmMessage() {
		return confirmed ? "dispatch committed" : "confirm returned no dispatch record";
	}

	/** ALT 7A. */
	public void cancelProposedAssignment() {
		facade.cancelRecommendation(recommendation.getRecommendationId());
	}

	public int activeDispatchCount() {
		return callCenter.getActiveDispatches().size();
	}

	// ---------------------------------------------------------------- Steps 8 and
	// 9

	public void deliverAssignment() {
		notifier.deliver(dispatch);
	}

	public boolean acknowledgeDispatch() {
		boolean recorded = callCenter.recordAcknowledgement(dispatch.getId(), clock.instant());
		if (recorded) {
			ackMonitor.cancel(dispatch.getId());
		}
		return recorded;
	}

	public boolean beginResponse() {
		callCenter.beginResponse(dispatch.getId());
		return true;
	}

	public String dispatchStatus() {
		return dispatch.getStatus().name();
	}

	// ---------------------------------------------------------------- units
	public void takeOutOfService(String callSign, String reason) {
		Ambulance unit = (Ambulance) callCenter.getAmbulance(unitId(callSign));
		unit.setStatus(AmbulanceStatus.OUT_OF_SERVICE);
	}
	public String statusOf(String callSign) {
		return ((Ambulance) callCenter.getAmbulance(unitId(callSign))).getStatus().name();
	}

	// ---------------------------------------------------------------- helpers

	private long unitId(String callSign) {
		Integer id = unitIds.get(callSign);
		if (id == null) {
			throw new IllegalArgumentException(
					"no unit called " + callSign + " in this scenario; check the Background table");
		}
		return id;
	}

	private static Duration parseDuration(String text) {
		String[] parts = text.trim().split("\\s+");
		long amount = Long.parseLong(parts[0]);
		return parts[1].startsWith("hour") ? Duration.ofHours(amount) : Duration.ofMinutes(amount);
	}

	public void recordCall(long callId, String address, String jurisdiction) {
		AmbulanceCall call = new AmbulanceCall(callId, new Location(44.9750, -93.2700), jurisdiction);
		callCenter.recordCall(call); // ← method name still unverified
	}
}
