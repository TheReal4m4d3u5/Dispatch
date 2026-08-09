package edu.ics240.dispatch.core;

/**
 * DTO representing what the crew sees on their device.
 * Built in Step 8 from DispatchRecord + crew info.
 */
public record CrewAssignment(
        long dispatchId,
        long callId,
        Location callLocation,
        String jurisdiction,
        Priority priority
) {}
