package edu.ics240.dispatch.core;

/**
 * Emergency-call priority.
 *
 * Lower rank values represent greater urgency.
 */
public enum Priority {

    CRITICAL(1),
    HIGH(2),
    MEDIUM(3),
    LOW(4),
    NON_EMERGENCY(5);

    private final int rank;

    Priority(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public static Priority fromRank(int rank) {
        for (Priority priority : values()) {
            if (priority.rank == rank) {
                return priority;
            }
        }

        throw new IllegalArgumentException(
                "Priority rank must be between 1 and 5: " + rank
        );
    }
}