package edu.ics240.dispatch.core;

/**
 * Domain Priority with explicit numeric rank.
 * Use getRank() instead of ordinal() to avoid brittle ordering.
 */
public enum Priority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int rank;

    Priority(int rank) {
        this.rank = rank;
    }

    /**
     * Numeric rank used for ordering. Higher number = higher urgency.
     */
    public int getRank() {
        return rank;
    }
}
