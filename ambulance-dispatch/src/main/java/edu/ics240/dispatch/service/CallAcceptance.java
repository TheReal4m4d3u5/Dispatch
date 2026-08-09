package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.AmbulanceCall;
import edu.ics240.dispatch.core.ValidationStatus;

/** Result of STEP 1. */
public record CallAcceptance(ValidationStatus status, AmbulanceCall call, String message) {

    public static CallAcceptance queued(AmbulanceCall call) {
        return new CallAcceptance(ValidationStatus.OK, call, "call queued for dispatch");
    }

    public static CallAcceptance noDispatchRequired(AmbulanceCall call) {
        return new CallAcceptance(ValidationStatus.CALL_NO_LONGER_REQUIRES_DISPATCH, call,
                "evaluation concluded no ambulance is required");
    }

    public static CallAcceptance notFound(long callId) {
        return new CallAcceptance(ValidationStatus.CALL_NOT_FOUND, null, "no such call: " + callId);
    }

    public boolean isOk() {
        return status == ValidationStatus.OK;
    }
}
