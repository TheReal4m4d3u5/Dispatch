package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.DispatchRecord;
import edu.ics240.dispatch.core.ValidationStatus;

/** Result of STEP 7. */
public record ConfirmOutcome(ValidationStatus status, DispatchRecord record, String message) {

    public static ConfirmOutcome dispatched(DispatchRecord record) {
        return new ConfirmOutcome(ValidationStatus.OK, record, "dispatch committed");
    }

    /** ALT 7B. The guarded transition refused; nothing mutated, nothing to roll back. */
    public static ConfirmOutcome rejected(String reason) {
        return new ConfirmOutcome(ValidationStatus.DISPATCH_REJECTED, null, reason);
    }

    public static ConfirmOutcome expired(long recommendationId) {
        return new ConfirmOutcome(ValidationStatus.RECOMMENDATION_EXPIRED, null,
                "recommendation " + recommendationId + " is no longer active");
    }

    public static ConfirmOutcome noAmbulanceSelected(long recommendationId) {
        return new ConfirmOutcome(ValidationStatus.NO_AMBULANCE_SELECTED, null,
                "recommendation " + recommendationId + " has no selected ambulance");
    }

    public boolean isOk() {
        return status == ValidationStatus.OK;
    }
}