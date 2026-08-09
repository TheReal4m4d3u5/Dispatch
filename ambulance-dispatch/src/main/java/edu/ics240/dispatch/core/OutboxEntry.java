package edu.ics240.dispatch.core;

import java.time.Instant;

/** One pending crew notification. */
public class OutboxEntry {

    private final long dispatchId;
    private final Instant enqueuedAt;
    private int attempts;
    private Instant lastAttemptAt;
    private boolean delivered;

    public OutboxEntry(long dispatchId, Instant enqueuedAt) {
        this.dispatchId = dispatchId;
        this.enqueuedAt = enqueuedAt;
    }

    public long getDispatchId() {
        return dispatchId;
    }

    public Instant getEnqueuedAt() {
        return enqueuedAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void recordAttempt(Instant at) {
        this.attempts++;
        this.lastAttemptAt = at;
    }

    public void markDelivered() {
        this.delivered = true;
    }

    @Override
    public String toString() {
        return "OutboxEntry[dispatch=" + dispatchId + " attempts=" + attempts
                + " delivered=" + delivered + "]";
    }
}