package edu.ics240.dispatch.core;

import java.time.LocalDateTime;

public class DispatchRecord {

    private final long dispatchId;
    private final AmbulanceCall call;
    private final Ambulance ambulance;
    private final LocalDateTime dispatchedAt;

    public DispatchRecord(
            long dispatchId,
            AmbulanceCall call,
            Ambulance ambulance) {

        this.dispatchId = dispatchId;
        this.call = call;
        this.ambulance = ambulance;
        this.dispatchedAt = LocalDateTime.now();
    }

    public long getDispatchId() {
        return dispatchId;
    }

    public AmbulanceCall getCall() {
        return call;
    }

    public Ambulance getAmbulance() {
        return ambulance;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }
}