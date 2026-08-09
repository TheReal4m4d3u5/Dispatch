package edu.ics240.dispatch.core;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction for the waiting calls priority queue with visibility lease support.
 * Implementations can be in-memory or backed by DB.
 */
public interface WaitingCalls {
    void add(AmbulanceCall call);
    void remove(AmbulanceCall call);
    Optional<AmbulanceCall> peekUnclaimed(long dispatcherId);
    void renewLease(AmbulanceCall call);
	List<AmbulanceCall> snapshot();
	void clear();
}