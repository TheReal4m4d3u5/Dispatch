package edu.ics240.dispatch.core;

import java.time.Instant;

/**
 * Immutable-ish record of a dispatch.
 * Step 7 creates this; Step 8 & 9 read and update status.
 */
public class DispatchRecord {

    public enum Status {
        ASSIGNED,       // created in Step 7
        ACKNOWLEDGED,   // crew acknowledged in Step 9
        IN_PROGRESS     // crew began responding in Step 9
    }

    private final long id;
    private final AmbulanceCall call;
    private final Ambulance ambulance;
    private final Instant dispatchedAt;

    private Status status;
    private Instant acknowledgedAt;

    public DispatchRecord(long id,
                          AmbulanceCall call,
                          Ambulance ambulance,
                          Instant dispatchedAt) {
        this.id = id;
        this.call = call;
        this.ambulance = ambulance;
        this.dispatchedAt = dispatchedAt;
        this.status = Status.ASSIGNED;
    }

    public long getId() { return id; }
    public AmbulanceCall getCall() { return call; }
    public Ambulance getAmbulance() { return ambulance; }
    public Status getStatus() { return status; }

    /**
     * Step 9: crew acknowledges within T.
     */
    public void markAcknowledged(Instant at) {
        if (status != Status.ASSIGNED) {
            throw new IllegalStateException("Cannot acknowledge; status is " + status);
        }
        this.status = Status.ACKNOWLEDGED;
        this.acknowledgedAt = at;
    }

    /**
     * Step 9: crew begins responding.
     */
    public void markInProgress() {
        if (status != Status.ACKNOWLEDGED) {
            throw new IllegalStateException("Cannot begin response; status is " + status);
        }
        this.status = Status.IN_PROGRESS;
    }

	public void markAssigned(Instant now) {
		// TODO Auto-generated method stub
		
	}
}
