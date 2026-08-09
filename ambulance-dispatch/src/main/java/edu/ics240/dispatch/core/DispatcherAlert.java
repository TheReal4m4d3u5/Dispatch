package edu.ics240.dispatch.core;

import java.time.Instant;

/** Something the dispatcher has to be told about after ED-01 has already returned. */
public record DispatcherAlert(long dispatchId, Kind kind, String message, Instant raisedAt) {

    public enum Kind {
        /** Delivery to the crew's device failed after every retry. */
        CREW_UNREACHABLE,
        /** Delivered, but nobody acknowledged inside the timeout. */
        CREW_NOT_RESPONDING
    }
}