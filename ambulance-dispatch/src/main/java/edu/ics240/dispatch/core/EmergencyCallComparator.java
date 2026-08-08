package edu.ics240.dispatch.core;

import java.util.Comparator;

public class EmergencyCallComparator
        implements Comparator<AmbulanceCall> {

    @Override
    public int compare(
            AmbulanceCall first,
            AmbulanceCall second) {

        int priorityComparison =
                Integer.compare(
                        first.getPriority().getRank(),
                        second.getPriority().getRank()
                );

        if (priorityComparison != 0) {
            return priorityComparison;
        }

        return Long.compare(
                first.getArrivalSequence(),
                second.getArrivalSequence()
        );
    }
}