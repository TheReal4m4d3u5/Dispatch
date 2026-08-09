package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.*;
import edu.ics240.dispatch.web.RecommendationController.RecommendationView;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * Application facade orchestrating ED-01 Steps 2–7. Controllers talk to this;
 * it talks to CallCenter + RecommendationService.
 */
@Service
public class AmbulanceDispatchFacade {

	private static final long DEFAULT_LEASE_SECONDS = 120;
	private final AmbulanceCallCenter callCenter;
	private final CadRecommendationService recommendationService;
	private final DispatchRecommendationRepository recommendationRepo;
	private final AmbulanceRepository ambulanceRepo;

	public AmbulanceDispatchFacade(AmbulanceCallCenter callCenter, CadRecommendationService recommendationService,
			DispatchRecommendationRepository recommendationRepo, AmbulanceRepository ambulanceRepo) {
		this.callCenter = callCenter;
		this.recommendationService = recommendationService;
		this.recommendationRepo = recommendationRepo;
		this.ambulanceRepo = ambulanceRepo;
	}

	/**
	 * STEP 2: recommendNext() for a dispatcher.
	 */
	public DispatchRecommendation recommendNext(long dispatcherId, long leaseSeconds) {
		AmbulanceCall call = callCenter.getNextWaitingCall(dispatcherId)
				.orElseThrow(() -> new IllegalStateException("No waiting calls."));

		List<Ambulance> eligible = callCenter.getEligibleAmbulances(call);
		if (eligible.isEmpty()) {
			// ALT 2A: no unit available
			// UI will show "No ambulance available"; call stays in waitingCalls.
			throw new IllegalStateException("No eligible ambulances.");
		}

		return recommendationService.recommend(call, eligible, leaseSeconds);
	}

	/**
	 * STEP 4: assemble recommendation view for dispatcher.
	 */
	public RecommendationView getRecommendationView(long recommendationId) {
		DispatchRecommendation rec = recommendationRepo.findById(recommendationId)
				.orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + recommendationId));

		Ambulance recommended = rec.getRecommendedAmbulance();
		List<Ambulance> alternatives = rec.getRankedAlternatives();

		return new RecommendationView(rec.getId(), rec.getCall().getId(), recommended.getId(),
				alternatives.stream().map(Ambulance::getId).toList());
	}

	/**
	 * STEP 5: dispatcher accepts CAD recommendation.
	 */
	public void selectRecommendedAmbulance(long recommendationId) {
		DispatchRecommendation rec = recommendationRepo.findById(recommendationId)
				.orElseThrow(() -> new NoSuchElementException("Recommendation not found: " + recommendationId));

		rec.select(rec.getRecommendedAmbulance(), SelectionSource.CAD_RECOMMENDED, null);
		recommendationRepo.save(rec);
	}

	/**
	 * STEP 5: dispatcher overrides recommendation.
	 */
	public void selectAlternateAmbulance(long recommendationId, long ambulanceId, OverrideReason overrideReason) {
		DispatchRecommendation rec = recommendationRepo.findById(recommendationId)
				.orElseThrow(() -> new NoSuchElementException("Recommendation not found: " + recommendationId));

		Ambulance ambulance = ambulanceRepo.findById(ambulanceId)
				.orElseThrow(() -> new NoSuchElementException("Ambulance not found: " + ambulanceId));

		rec.select(ambulance, SelectionSource.DISPATCHER_OVERRIDE, overrideReason);
		recommendationRepo.save(rec);
	}

	/**
	 * Convenience overload for the web layer, which receives the reason as text.
	 */
	public void selectAlternateAmbulance(long recommendationId, long ambulanceId, String reasonCode) {
		selectAlternateAmbulance(recommendationId, ambulanceId, OverrideReason.valueOf(reasonCode));
	}

	/**
	 * STEP 6: validate selected ambulance and call.
	 */
	public ValidationStatus validateDispatchSelection(long recommendationId) {
		return validateSelection(recommendationId).status();
	}
	
	/** STEP 6, outcome-returning variant used by the BDD harness. */
	public ValidationOutcome validateSelection(long recommendationId) {
		DispatchRecommendation rec = recommendationRepo.findById(recommendationId)
				.orElseThrow(() -> new NoSuchElementException("Recommendation not found: " + recommendationId));

		if (rec.isLeaseExpired()) {
			return ValidationOutcome.expired(recommendationId);
		}

		AmbulanceCall call = rec.getCall();
		if (!call.isReadyForDispatch()) {
			return ValidationOutcome.callNoLongerRequiresDispatch(call.getId());
		}

		Ambulance selected = rec.getSelectedAmbulance();
		if (selected == null) {
			return ValidationOutcome.noAmbulanceSelected(recommendationId);
		}

		if (!selected.isDispatchable() || !selected.isAppropriateFor(call)) {
			// ALT 6A: rebuild over whatever is still eligible, call stays queued
			recommendationRepo.delete(rec);
			List<Ambulance> eligible = callCenter.getEligibleAmbulances(call);
			DispatchRecommendation replacement = eligible.isEmpty()
					? null
					: recommendationService.recommend(call, eligible, DEFAULT_LEASE_SECONDS);
			callCenter.renewLease(call);
			return ValidationOutcome.ambulanceUnavailable(replacement,
					selected.getCallSign() + " is no longer available");
		}

		call.cancelDispatchRequirement();
		callCenter.removeFromQueue(call);
		return ValidationOutcome.valid();
	}

	/**
	 * STEP 7: confirm dispatch.
	 */
	public DispatchRecord confirmDispatch(long recommendationId) {
		DispatchRecommendation rec = recommendationRepo.findById(recommendationId)
				.orElseThrow(() -> new NoSuchElementException("Recommendation not found: " + recommendationId));

		return callCenter.confirmDispatch(rec);
	}

	/**
	 * STEP 7: cancel recommendation instead of confirming.
	 */
	public void cancelRecommendation(long recommendationId) {
		DispatchRecommendation rec = recommendationRepo.findById(recommendationId)
				.orElseThrow(() -> new NoSuchElementException("Recommendation not found: " + recommendationId));

		AmbulanceCall call = rec.getCall();

		// Release lease and remove recommendation; call stays in waitingCalls.
		callCenter.renewLease(call);
		recommendationRepo.delete(rec);
	}

	public void completeEvaluation(long callId, EvaluationResult result) {
		AmbulanceCall call = callCenter.getCall(callId);
		if (call == null) {
			throw new NoSuchElementException("Call not found: " + callId);
		}
		callCenter.completeEvaluation(call, result.priority(), result.requiredCapability().name(),
				result.jurisdiction(), result.requiresDispatch());
	}

	/** STEP 2, outcome-returning variant used by the BDD harness. */
	public RecommendationOutcome recommendNext(String dispatcherId) {
		long id = dispatcherId == null ? 0L : dispatcherId.hashCode();

		Optional<AmbulanceCall> next = callCenter.getNextWaitingCall(id);
		if (next.isEmpty()) {
			return RecommendationOutcome.noWaitingCall();
		}
		AmbulanceCall call = next.get();

		List<Ambulance> eligible = callCenter.getEligibleAmbulances(call);
		if (eligible.isEmpty()) {
			return RecommendationOutcome.noEligibleAmbulance(call.getId());
		}

		DispatchRecommendation rec = recommendationService.recommend(call, eligible, DEFAULT_LEASE_SECONDS);
		recommendationRepo.save(rec);
		return RecommendationOutcome.ok(rec);
	}

}
