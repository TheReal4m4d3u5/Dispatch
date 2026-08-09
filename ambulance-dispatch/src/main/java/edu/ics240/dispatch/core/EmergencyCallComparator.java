package edu.ics240.dispatch.core;

import java.util.Comparator;

/**
 * Compare AmbulanceCall by Priority (rank) then by arrival sequence.
 * Null-safe: null calls are treated as lowest priority and placed last.
 * Sorts by priority descending (CRITICAL first), then by arrivalSequence ascending (older first).
 */
public class EmergencyCallComparator implements Comparator<AmbulanceCall> {

    @Override
    public int compare(AmbulanceCall first, AmbulanceCall second) {
        if (first == second) return 0;
        if (first == null) return 1;   // nulls last
        if (second == null) return -1;

        Priority p1 = first.getPriority();
        Priority p2 = second.getPriority();

        int r1 = (p1 == null) ? Integer.MIN_VALUE : p1.getRank();
        int r2 = (p2 == null) ? Integer.MIN_VALUE : p2.getRank();

        // Descending by rank: higher rank value should come first
        int priorityComparison = Integer.compare(r2, r1);

        if (priorityComparison != 0) {
            return priorityComparison;
        }

        // Tie-breaker: arrival sequence (older first)
        return Long.compare(first.getArrivalSequence(), second.getArrivalSequence());
    }
}
