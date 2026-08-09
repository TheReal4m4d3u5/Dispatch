package edu.ics240.dispatch.inmemory;

import edu.ics240.dispatch.core.DispatchOutbox;
import edu.ics240.dispatch.core.DispatchRecord;
import edu.ics240.dispatch.core.OutboxEntry;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Profile("!jpa")
public class InMemoryDispatchOutbox implements DispatchOutbox {

    private final ConcurrentLinkedQueue<Long> pending = new ConcurrentLinkedQueue<>();
    private final Map<Long, OutboxEntry> entries = new ConcurrentHashMap<>();
    private final Map<Long, DispatchRecord> payloads = new ConcurrentHashMap<>();

    @Override
    public void enqueue(DispatchRecord record, Instant at) {
        Objects.requireNonNull(record, "record");
        long dispatchId = record.getId();
        entries.put(dispatchId, new OutboxEntry(dispatchId, at));
        payloads.put(dispatchId, record);
        pending.add(dispatchId);
    }

    @Override
    public Optional<DispatchRecord> claimNext() {
        Long dispatchId = pending.poll();
        while (dispatchId != null) {
            OutboxEntry entry = entries.get(dispatchId);
            if (entry != null && !entry.isDelivered()) {
                return Optional.ofNullable(payloads.get(dispatchId));
            }
            dispatchId = pending.poll();
        }
        return Optional.empty();
    }

    @Override
    public void requeue(long dispatchId) {
        pending.add(dispatchId);
    }

    @Override
    public void recordAttempt(long dispatchId, Instant at) {
        OutboxEntry entry = entries.get(dispatchId);
        if (entry != null) {
            entry.recordAttempt(at);
        }
    }

    @Override
    public void markDelivered(long dispatchId) {
        OutboxEntry entry = entries.get(dispatchId);
        if (entry != null) {
            entry.markDelivered();
        }
    }

    @Override
    public int attemptsFor(long dispatchId) {
        OutboxEntry entry = entries.get(dispatchId);
        return entry == null ? 0 : entry.getAttempts();
    }

    @Override
    public List<OutboxEntry> undelivered() {
        List<OutboxEntry> out = new ArrayList<>();
        for (OutboxEntry entry : entries.values()) {
            if (!entry.isDelivered()) {
                out.add(entry);
            }
        }
        return out;
    }

    @Override
    public int pendingCount() {
        return pending.size();
    }
}
