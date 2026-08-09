package edu.ics240.dispatch.core;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Port. Handoff between the dispatch commit and the crew notification.
 *
 * <p>The entry is written inside the same transaction as the commit, so it cannot exist
 * without a committed dispatch and a committed dispatch cannot exist without it. That
 * ordering rules out both bad shapes: notifying inside the transaction, where a radio
 * outage rolls back a valid dispatch, and notifying naively after it, where a committed
 * dispatch can silently reach nobody.
 */
public interface DispatchOutbox {

    void enqueue(DispatchRecord record, Instant at);

    Optional<DispatchRecord> claimNext();

    void requeue(long dispatchId);

    void recordAttempt(long dispatchId, Instant at);

    void markDelivered(long dispatchId);

    int attemptsFor(long dispatchId);

    List<OutboxEntry> undelivered();

    int pendingCount();
}