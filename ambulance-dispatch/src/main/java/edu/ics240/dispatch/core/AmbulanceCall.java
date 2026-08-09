package edu.ics240.dispatch.core;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AmbulanceCall domain entity (complete enough for comparator, queueing and basic dispatch).
 * Keep domain rules here; persistence mapping is handled elsewhere.
 */
public class AmbulanceCall {

    private static final AtomicLong SEQUENCE_GENERATOR = new AtomicLong(1);

    private final long id;
    private final long arrivalSequence;
    private CallState state;
    private Priority priority;
    private String requiredCapability;
    private String jurisdiction;
    private boolean requiresDispatch;
    private Location location;
    private Instant createdAt;
    private Long assignedAmbulanceId; // nullable: which ambulance was assigned
    private Instant dispatchedAt;     // nullable: when the ambulance was dispatched

    public AmbulanceCall(long id, Location location, String jurisdiction) {
        this.id = id;
        this.location = location;
        this.jurisdiction = jurisdiction;
        this.state = CallState.NEW;
        this.createdAt = Instant.now();
        this.arrivalSequence = SEQUENCE_GENERATOR.getAndIncrement();
        this.requiresDispatch = false;
    }



	public long getId() {
        return id;
    }

    /**
     * Monotonic arrival sequence used for deterministic ordering.
     */
    public long getArrivalSequence() {
        return arrivalSequence;
    }

    public CallState getState() {
        return state;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getRequiredCapability() {
        return requiredCapability;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public boolean isReadyForDispatch() {
        return state == CallState.READY_FOR_DISPATCH;
    }
    
    public boolean isRequiresDispatch() {
        return requiresDispatch;
    }

    public Location getLocation() {
        return location;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getAssignedAmbulanceId() {
        return assignedAmbulanceId;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    // -------------------------
    // Domain behavior
    // -------------------------

    /**
     * Complete evaluation of the call: set priority, capability, jurisdiction and whether dispatch is required.
     * This method models the domain decision that determines if the call should enter the waiting queue.
     */
    public void completeEvaluation(Priority priority,
                                   String requiredCapability,
                                   String jurisdiction,
                                   boolean requiresDispatch) {
        this.priority = priority;
        this.requiredCapability = requiredCapability;
        this.jurisdiction = jurisdiction;
        this.requiresDispatch = requiresDispatch;
        if (requiresDispatch) {
            this.state = CallState.READY_FOR_DISPATCH;
        } else {
            this.state = CallState.EVALUATED;
        }
    }

    /**
     * Mark the call ready for dispatch (explicit domain intent).
     */
    public void markReadyForDispatch() {
        this.requiresDispatch = true;
        this.state = CallState.READY_FOR_DISPATCH;
    }

    /**
     * Cancel the dispatch requirement and remove from any queue.
     */
    public void cancelDispatchRequirement() {
        this.requiresDispatch = false;
        if (this.state == CallState.READY_FOR_DISPATCH) {
            this.state = CallState.EVALUATED;
        }
    }
    
    



    /**
     * Assign an ambulance to this call by ambulance id.
     * Keep this method for callers that only have the id.
     */
    public void assignTo(long ambulanceId) {
        this.assignedAmbulanceId = ambulanceId;
        this.dispatchedAt = Instant.now();
        this.state = CallState.ASSIGNED;
        this.requiresDispatch = false;
    }

    /**
     * Overloaded assignTo that accepts an Ambulance domain object and an explicit dispatch time.
     * Use this when the caller has the Ambulance instance and a specific dispatch timestamp.
     */
    public void assignTo(Ambulance ambulance, Instant dispatchedAt) {
        if (ambulance == null) {
            throw new IllegalArgumentException("ambulance must not be null");
        }
        this.assignedAmbulanceId = ambulance.getId();
        this.dispatchedAt = dispatchedAt;
        this.state = CallState.ASSIGNED;
        this.requiresDispatch = false;
    }

    /**
     * Unassign ambulance (e.g., lease expired or assignment cancelled).
     */
    public void unassign() {
        this.assignedAmbulanceId = null;
        this.dispatchedAt = null;
        this.state = CallState.EVALUATED;
    }

    // -------------------------
    // Utility
    // -------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AmbulanceCall)) return false;
        AmbulanceCall that = (AmbulanceCall) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AmbulanceCall{" +
                "id=" + id +
                ", arrivalSequence=" + arrivalSequence +
                ", state=" + state +
                ", priority=" + priority +
                ", jurisdiction='" + jurisdiction + '\'' +
                ", requiresDispatch=" + requiresDispatch +
                ", assignedAmbulanceId=" + assignedAmbulanceId +
                ", dispatchedAt=" + dispatchedAt +
                '}';
    }
}
