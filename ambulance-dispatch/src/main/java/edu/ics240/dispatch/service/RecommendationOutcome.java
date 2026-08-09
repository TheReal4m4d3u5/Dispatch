package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.DispatchRecommendation;
import edu.ics240.dispatch.core.ValidationStatus;

/** Result of STEP 2 and STEP 3 taken together. */
public record RecommendationOutcome(ValidationStatus status,
                                    DispatchRecommendation recommendation,
                                    String message) {

    public static RecommendationOutcome ok(DispatchRecommendation recommendation) {
        return new RecommendationOutcome(ValidationStatus.OK, recommendation, "recommendation created");
    }

    public static RecommendationOutcome noWaitingCall() {
        return new RecommendationOutcome(ValidationStatus.NO_WAITING_CALL, null,
                "no unclaimed call is waiting for dispatch");
    }

    /** ALT 2A. Every unit is committed; the call stays queued at its original priority. */
    public static RecommendationOutcome noEligibleAmbulance(long callId) {
        return new RecommendationOutcome(ValidationStatus.NO_ELIGIBLE_AMBULANCE, null,
                "no available and appropriate unit for call " + callId + "; call remains queued");
    }

    public boolean isOk() {
        return status == ValidationStatus.OK;
    }
}
