package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.DispatchRecommendation;
import edu.ics240.dispatch.core.ValidationStatus;

/**
 * Result of STEP 6. Advisory: passing here says the selection was sound a moment ago,
 * not that it will still be sound at commit. Only STEP 7 is authoritative.
 */
public record ValidationOutcome(ValidationStatus status,
                                DispatchRecommendation replacement,
                                String message) {

    public static ValidationOutcome valid() {
        return new ValidationOutcome(ValidationStatus.OK, null, "selection validated");
    }

    /** ALT 6A. A replacement is supplied so the caller returns to STEP 4. */
    public static ValidationOutcome ambulanceUnavailable(DispatchRecommendation replacement, String detail) {
        return new ValidationOutcome(ValidationStatus.AMBULANCE_UNAVAILABLE, replacement, detail);
    }

    /** ALT 6B. ED-01 ends. */
    public static ValidationOutcome callNoLongerRequiresDispatch(long callId) {
        return new ValidationOutcome(ValidationStatus.CALL_NO_LONGER_REQUIRES_DISPATCH, null,
                "call " + callId + " no longer requires dispatch");
    }

    public static ValidationOutcome expired(long recommendationId) {
        return new ValidationOutcome(ValidationStatus.RECOMMENDATION_EXPIRED, null,
                "recommendation " + recommendationId + " is no longer active");
    }

    public static ValidationOutcome noAmbulanceSelected(long recommendationId) {
        return new ValidationOutcome(ValidationStatus.NO_AMBULANCE_SELECTED, null,
                "recommendation " + recommendationId + " has no selected ambulance");
    }

    public boolean isOk() {
        return status == ValidationStatus.OK;
    }
}