package edu.ics240.dispatch.core;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents one completed transition in an ambulance's lifecycle.
 *
 * Examples:
 *
 * AVAILABLE -> DISPATCHED
 * DISPATCHED -> ON_SCENE
 * AVAILABLE -> MAINTENANCE
 * MAINTENANCE -> AVAILABLE
 *
 * Each Ambulance object will keep an append-only history of these
 * objects inside an ArrayList.
 *
 * This class is immutable:
 *
 * 1. Every field is private and final.
 * 2. There are no setter methods.
 * 3. AmbulanceStatus is an enum and therefore immutable.
 * 4. Instant is immutable.
 * 5. String is immutable.
 *
 * Immutability is important because a state-history entry represents
 * something that already happened. Historical data should not be
 * changed after it has been recorded.
 *
 * Data-structure use:
 *
 * Ambulance will eventually contain:
 *
 *     ArrayList<AmbulanceStateChange> stateHistory;
 *
 * Appending one state change to that ArrayList will have an amortized
 * time complexity of O(1).
 *
 * Reading all state changes for one ambulance will have a time
 * complexity of O(s), where s is the number of entries in that
 * ambulance's state history.
 */
public final class AmbulanceStateChange {

    /**
     * The ambulance state before the transition occurred.
     *
     * Example:
     *
     *     AVAILABLE
     */
    private final AmbulanceStatus previousStatus;

    /**
     * The ambulance state after the transition occurred.
     *
     * Example:
     *
     *     DISPATCHED
     */
    private final AmbulanceStatus newStatus;

    /**
     * The exact time at which the state transition occurred.
     *
     * Instant is used instead of a formatted String so that the
     * application can:
     *
     * - compare timestamps;
     * - calculate durations;
     * - format the value differently for the console or web page;
     * - use a controllable Clock during automated tests.
     */
    private final Instant changedAt;

    /**
     * A human-readable explanation of why the state changed.
     *
     * Examples:
     *
     * - "Assigned to emergency call 17"
     * - "Arrived at patient location"
     * - "Brake inspection required"
     * - "Qualified crew assigned"
     */
    private final String reason;

    /**
     * Creates one immutable ambulance state-history entry.
     *
     * All validation occurs in the constructor so that an invalid
     * AmbulanceStateChange object can never be created.
     *
     * Time complexity:
     *
     *     O(1)
     *
     * The constructor performs only a fixed number of null checks,
     * comparisons, and String operations.
     *
     * Space complexity:
     *
     *     O(1)
     *
     * Each state-change object stores a fixed number of fields.
     *
     * @param previousStatus the ambulance state before the transition
     * @param newStatus      the ambulance state after the transition
     * @param changedAt      the exact time the transition occurred
     * @param reason         a human-readable explanation of the change
     *
     * @throws NullPointerException if either status or changedAt is null
     * @throws IllegalArgumentException if both statuses are identical
     *                                  or the reason is blank
     */
    public AmbulanceStateChange(
            AmbulanceStatus previousStatus,
            AmbulanceStatus newStatus,
            Instant changedAt,
            String reason) {

        /*
         * A valid history entry must always identify the state that
         * existed before the transition.
         */
        this.previousStatus = Objects.requireNonNull(
                previousStatus,
                "Previous ambulance status cannot be null"
        );

        /*
         * A valid history entry must always identify the state that
         * exists after the transition.
         */
        this.newStatus = Objects.requireNonNull(
                newStatus,
                "New ambulance status cannot be null"
        );

        /*
         * A transition must actually move the ambulance from one state
         * to a different state.
         *
         * Recording AVAILABLE -> AVAILABLE would not represent a real
         * lifecycle transition and could hide a programming error.
         */
        if (previousStatus == newStatus) {
            throw new IllegalArgumentException(
                    "Previous status and new status must be different"
            );
        }

        /*
         * Every history entry requires a timestamp so the application
         * can determine when transitions occurred and calculate elapsed
         * time between lifecycle milestones.
         */
        this.changedAt = Objects.requireNonNull(
                changedAt,
                "State-change time cannot be null"
        );

        /*
         * A reason provides useful audit and debugging information.
         *
         * The helper method also trims leading and trailing whitespace
         * before the value is stored.
         */
        this.reason = requireText(
                reason,
                "State-change reason"
        );
    }

    /**
     * Returns the state that existed before the transition.
     *
     * Time complexity:
     *
     *     O(1)
     *
     * @return the previous ambulance status
     */
    public AmbulanceStatus getPreviousStatus() {
        return previousStatus;
    }

    /**
     * Returns the state that exists after the transition.
     *
     * Time complexity:
     *
     *     O(1)
     *
     * @return the new ambulance status
     */
    public AmbulanceStatus getNewStatus() {
        return newStatus;
    }

    /**
     * Returns the time at which the transition occurred.
     *
     * Instant is immutable, so returning this reference does not allow
     * callers to modify the stored timestamp.
     *
     * Time complexity:
     *
     *     O(1)
     *
     * @return the immutable transition timestamp
     */
    public Instant getChangedAt() {
        return changedAt;
    }

    /**
     * Returns the explanation for the state transition.
     *
     * String is immutable, so returning it does not expose mutable
     * internal state.
     *
     * Time complexity:
     *
     *     O(1)
     *
     * @return the nonblank transition reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Validates a required text value.
     *
     * This helper method prevents null, empty, or whitespace-only
     * reasons from being stored.
     *
     * Examples of rejected values:
     *
     *     null
     *     ""
     *     "     "
     *
     * The accepted value is trimmed before being returned.
     *
     * @param value     the text being validated
     * @param fieldName the field name used in the error message
     * @return the trimmed, nonblank text
     *
     * @throws IllegalArgumentException if the value is null or blank
     */
    private static String requireText(
            String value,
            String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank"
            );
        }

        return value.trim();
    }

    /**
     * Compares this state-history entry with another object.
     *
     * Two entries are considered equal when they contain the same:
     *
     * - previous status;
     * - new status;
     * - timestamp;
     * - reason.
     *
     * This is useful in unit tests when an expected history entry is
     * compared with the entry stored by an Ambulance.
     *
     * Time complexity:
     *
     *     O(1) for the fixed fields, excluding the bounded String
     *     comparison cost.
     *
     * @param other the object being compared
     * @return true when both objects represent the same state change
     */
    @Override
    public boolean equals(Object other) {

        /*
         * An object is always equal to itself.
         */
        if (this == other) {
            return true;
        }

        /*
         * Pattern matching verifies the type and creates a correctly
         * typed local variable in one operation.
         */
        if (!(other instanceof AmbulanceStateChange stateChange)) {
            return false;
        }

        return previousStatus == stateChange.previousStatus
                && newStatus == stateChange.newStatus
                && changedAt.equals(stateChange.changedAt)
                && reason.equals(stateChange.reason);
    }

    /**
     * Produces a hash code consistent with equals().
     *
     * This allows state-change objects to work correctly if they are
     * ever stored in a hash-based structure such as HashSet or used as
     * keys in a HashMap.
     *
     * Time complexity:
     *
     *     O(1)
     *
     * @return a hash code based on every value field
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                previousStatus,
                newStatus,
                changedAt,
                reason
        );
    }

    /**
     * Returns a readable representation of the state transition.
     *
     * This is useful for:
     *
     * - console demonstrations;
     * - debugging;
     * - log messages;
     * - test-failure output.
     *
     * Example:
     *
     * AmbulanceStateChange{
     *     previousStatus=AVAILABLE,
     *     newStatus=DISPATCHED,
     *     changedAt=2026-08-06T18:30:00Z,
     *     reason='Assigned to emergency call 4'
     * }
     *
     * @return a readable description of this history entry
     */
    @Override
    public String toString() {
        return "AmbulanceStateChange{"
                + "previousStatus=" + previousStatus
                + ", newStatus=" + newStatus
                + ", changedAt=" + changedAt
                + ", reason='" + reason + '\''
                + '}';
    }
}

