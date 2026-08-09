package edu.ics240.dispatch.core;

/** Justification required when a dispatcher rejects the recommended unit. */
public enum OverrideReason {
    LOCAL_KNOWLEDGE,
    CREW_CAPABILITY,
    ROAD_CONDITIONS,
    UNIT_STATUS_STALE,
    OTHER
}
