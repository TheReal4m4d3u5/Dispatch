package edu.ics240.dispatch.core;

/** Lifecycle of a committed dispatch. */
public enum DispatchStatus {
    CREATED,
    ASSIGNED,
    ACKNOWLEDGED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
