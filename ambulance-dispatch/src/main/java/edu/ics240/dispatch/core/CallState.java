package edu.ics240.dispatch.core;

/**
 * Lifecycle of an AmbulanceCall in ED-01.
 * Step 1 moves NEW -> EVALUATED/READY_FOR_DISPATCH.
 * Step 7 moves READY_FOR_DISPATCH -> DISPATCHED.
 */
public enum CallState {
    NEW,
    EVALUATED,
    READY_FOR_DISPATCH,
    DISPATCHED,
    COMPLETED,
    CANCELLED, ASSIGNED
}
