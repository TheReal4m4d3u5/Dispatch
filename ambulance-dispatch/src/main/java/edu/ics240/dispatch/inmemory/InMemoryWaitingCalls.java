package edu.ics240.dispatch.inmemory;

import edu.ics240.dispatch.core.AmbulanceCall;
import edu.ics240.dispatch.core.WaitingCalls;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Very simple in-memory waiting calls queue with a naive lease model.
 * For production you will replace this with a DB-backed queue that supports visibility leases.
 */
@Profile("inmemory")
@Component
public class InMemoryWaitingCalls implements WaitingCalls {

    private final PriorityQueue<AmbulanceCall> queue = new PriorityQueue<>(Comparator.comparing(AmbulanceCall::getPriority));
    private final Map<Long, Long> leases = new ConcurrentHashMap<>(); // callId -> leaseExpiryEpochSec

    @Override
    public synchronized void add(AmbulanceCall call) {
        queue.remove(call); // ensure no duplicates
        queue.add(call);
    }

    
    @Override
    public synchronized void clear() {
        queue.clear();
        leases.clear();
    }
    
    @Override
    public synchronized void remove(AmbulanceCall call) {
        queue.remove(call);
        leases.remove(call.getId());
    }

    @Override
    public synchronized Optional<AmbulanceCall> peekUnclaimed(long dispatcherId) {
        AmbulanceCall next = queue.peek();
        if (next == null) return Optional.empty();
        long now = System.currentTimeMillis() / 1000;
        Long leaseExpiry = leases.get(next.getId());
        if (leaseExpiry == null || leaseExpiry < now) {
            // grant a short lease (e.g., 120s)
            leases.put(next.getId(), now + 120);
            return Optional.of(next);
        }
        return Optional.empty();
    }

    @Override
    public synchronized void renewLease(AmbulanceCall call) {
        long now = System.currentTimeMillis() / 1000;
        leases.put(call.getId(), now + 120);
    }

    @Override
    public synchronized List<AmbulanceCall> snapshot() {
        List<AmbulanceCall> copy = new ArrayList<>(queue);
        copy.sort(queue.comparator());
        return copy;
    }
}
