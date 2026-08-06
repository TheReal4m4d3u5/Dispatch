package edu.ics240.dispatch.core;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents one emergency call received by the ambulance call center.
 *
 * AmbulanceCall is immutable because its priority and arrival sequence
 * determine its position inside the waiting-call min heap.
 */
public final class AmbulanceCall {

    private final long callId;
    private final String callerName;
    private final String description;
    private final Priority priority;
    private final Location location;
    private final Instant receivedAt;
    private final long arrivalSequence;

    /**
     * Creates an immutable emergency call.
     *
     * @param callId          unique positive call identifier
     * @param callerName      name of the caller
     * @param description     description of the emergency
     * @param priority        emergency priority
     * @param location        location of the emergency
     * @param receivedAt      time the call was received
     * @param arrivalSequence monotonic FCFS sequence
     */
    public AmbulanceCall(
            long callId,
            String callerName,
            String description,
            Priority priority,
            Location location,
            Instant receivedAt,
            long arrivalSequence) {

        if (callId <= 0) {
            throw new IllegalArgumentException(
                    "Call ID must be positive"
            );
        }

        if (arrivalSequence < 0) {
            throw new IllegalArgumentException(
                    "Arrival sequence cannot be negative"
            );
        }

        this.callId = callId;
        this.callerName = requireText(
                callerName,
                "Caller name"
        );
        this.description = requireText(
                description,
                "Description"
        );
        this.priority = Objects.requireNonNull(
                priority,
                "Priority cannot be null"
        );
        this.location = Objects.requireNonNull(
                location,
                "Location cannot be null"
        );
        this.receivedAt = Objects.requireNonNull(
                receivedAt,
                "Received time cannot be null"
        );
        this.arrivalSequence = arrivalSequence;
    }

    public long getCallId() {
        return callId;
    }

    public String getCallerName() {
        return callerName;
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }

    public Location getLocation() {
        return location;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public long getArrivalSequence() {
        return arrivalSequence;
    }

    /**
     * Creates a replacement call with a different priority.
     *
     * The original call ID, timestamp, and arrival sequence are
     * preserved. This prevents escalation from resetting the call's
     * first-come-first-served position among calls of the same priority.
     *
     * The call center must remove the original call from the heap before
     * inserting the replacement.
     *
     * @param newPriority replacement priority
     * @return a new immutable AmbulanceCall
     */
    public AmbulanceCall withPriority(
            Priority newPriority) {

        Objects.requireNonNull(
                newPriority,
                "New priority cannot be null"
        );

        return new AmbulanceCall(
                callId,
                callerName,
                description,
                newPriority,
                location,
                receivedAt,
                arrivalSequence
        );
    }

    private static String requireText(
            String value,
            String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank"
            );
        }

        return value.trim();
    }

    @Override
    public String toString() {
        return "AmbulanceCall{"
                + "callId=" + callId
                + ", callerName='" + callerName + '\''
                + ", priority=" + priority
                + ", location=" + location
                + ", receivedAt=" + receivedAt
                + ", arrivalSequence=" + arrivalSequence
                + '}';
    }
}