package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.DispatchRecommendation;
import edu.ics240.dispatch.core.ValidationStatus;

/** Result of STEP 5. */
public record SelectionOutcome(ValidationStatus status,
                               DispatchRecommendation recommendation,
                               String message) {

    public static SelectionOutcome accepted(DispatchRecommendation recommendation) {
        return new SelectionOutcome(ValidationStatus.OK, recommendation, "selection recorded");
    }

    public static SelectionOutcome expired(long recommendationId) {
        return new SelectionOutcome(ValidationStatus.RECOMMENDATION_EXPIRED, null,
                "recommendation " + recommendationId + " is no longer active");
    }

    public static SelectionOutcome ambulanceNotFound(int ambulanceId) {
        return new SelectionOutcome(ValidationStatus.AMBULANCE_NOT_FOUND, null,
                "no such ambulance: " + ambulanceId);
    }

    public boolean isOk() {
        return status == ValidationStatus.OK;
    }
}