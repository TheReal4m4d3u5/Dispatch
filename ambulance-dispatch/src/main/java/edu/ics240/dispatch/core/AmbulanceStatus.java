package edu.ics240.dispatch.core;

/**
 * Represents the operational lifecycle state of an ambulance.
 *
 * Only AVAILABLE ambulances may be assigned to emergency calls.
 */
public enum AmbulanceStatus {

    AVAILABLE(true, false),

    DISPATCHED(false, true),
    ON_SCENE(false, true),
    TRANSPORTING(false, true),
    AT_HOSPITAL(false, true),

    RETURNING_TO_SERVICE(false, false),
    CLEANING_AND_RESTOCKING(false, false),
    MAINTENANCE(false, false),
    UNSTAFFED(false, false),
    OUT_OF_SERVICE(false, false);

    private final boolean dispatchable;
    private final boolean activeCall;

    AmbulanceStatus(
            boolean dispatchable,
            boolean activeCall) {

        this.dispatchable = dispatchable;
        this.activeCall = activeCall;
    }

    public boolean isDispatchable() {
        return dispatchable;
    }

    public boolean hasActiveCall() {
        return activeCall;
    }
}