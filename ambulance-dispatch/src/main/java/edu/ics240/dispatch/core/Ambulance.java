package edu.ics240.dispatch.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Represents one ambulance registered with the dispatch system.
 *
 * <p>An Ambulance is a mutable domain entity because its operational
 * condition changes throughout the program. For example, the same
 * ambulance may move through this lifecycle:</p>
 *
 * <pre>
 * AVAILABLE
 *     -> DISPATCHED
 *     -> ON_SCENE
 *     -> TRANSPORTING
 *     -> AT_HOSPITAL
 *     -> CLEANING_AND_RESTOCKING
 *     -> AVAILABLE
 * </pre>
 *
 * <p>This class is the GRASP Information Expert for an ambulance's
 * individual lifecycle because it owns:</p>
 *
 * <ul>
 *     <li>the ambulance's current status;</li>
 *     <li>its current location;</li>
 *     <li>its active emergency call;</li>
 *     <li>the time it most recently became available;</li>
 *     <li>its state-transition history.</li>
 * </ul>
 *
 * <p>The class does not expose a general-purpose {@code setStatus}
 * method. Instead, it provides meaningful domain operations such as
 * {@link #assignTo(AmbulanceCall, Instant)},
 * {@link #arriveOnScene(Instant)}, and
 * {@link #sendToMaintenance(Instant, String)}.</p>
 *
 * <p>This prevents outside classes from placing an ambulance into an
 * illegal state.</p>
 *
 * <p>This class implements a finite-state machine using an enum and
 * guarded methods. It does not use the GoF State design pattern.</p>
 */
public final class Ambulance {

    /**
     * Defines every legal ambulance-status transition.
     *
     * <p>An EnumMap is used because every key is an AmbulanceStatus
     * enum value. EnumMap is compact and provides constant-time access
     * for enum keys.</p>
     *
     * <p>Each map value is an EnumSet containing the statuses that may
     * legally follow the key status.</p>
     *
     * <p>Example:</p>
     *
     * <pre>
     * AVAILABLE -> DISPATCHED
     * AVAILABLE -> MAINTENANCE
     * AVAILABLE -> UNSTAFFED
     * AVAILABLE -> OUT_OF_SERVICE
     * </pre>
     *
     * <p>Time complexity for checking one transition is O(1).</p>
     */
    private static final EnumMap<
            AmbulanceStatus,
            EnumSet<AmbulanceStatus>
            > LEGAL_TRANSITIONS = createLegalTransitions();

    /**
     * Unique positive identifier for this ambulance.
     *
     * <p>The call center will use this value as the key in:</p>
     *
     * <pre>
     * HashMap&lt;Integer, Ambulance&gt;
     * </pre>
     *
     * <p>Once assigned, the ID never changes.</p>
     */
    private final int ambulanceId;

    /**
     * Human-readable ambulance name or unit designation.
     *
     * <p>Examples:</p>
     *
     * <pre>
     * Medic 1
     * Unit 204
     * Downtown Ambulance
     * </pre>
     */
    private final String name;

    /**
     * Current known location of the ambulance.
     *
     * <p>Location is immutable, so replacing this reference is safe.
     * The existing Location object cannot be modified by another
     * class.</p>
     */
    private Location location;

    /**
     * Current operational lifecycle state.
     *
     * <p>Only {@link AmbulanceStatus#AVAILABLE} is dispatchable.</p>
     */
    private AmbulanceStatus status;

    /**
     * Emergency call currently assigned to this ambulance.
     *
     * <p>This field must be non-null when the status is:</p>
     *
     * <ul>
     *     <li>DISPATCHED;</li>
     *     <li>ON_SCENE;</li>
     *     <li>TRANSPORTING;</li>
     *     <li>AT_HOSPITAL.</li>
     * </ul>
     *
     * <p>It must be null for all other statuses.</p>
     */
    private AmbulanceCall activeCall;

    /**
     * Time at which this ambulance most recently entered AVAILABLE.
     *
     * <p>This field supports deterministic tie-breaking when multiple
     * ambulances are approximately the same distance from a call.</p>
     *
     * <p>The ambulance that has been available the longest is selected
     * first.</p>
     *
     * <p>This value changes only when the ambulance enters AVAILABLE.
     * Other transitions do not modify it.</p>
     */
    private Instant availableSince;

    /**
     * Append-only history of lifecycle transitions.
     *
     * <p>ArrayList is appropriate because state changes are appended
     * and later read sequentially.</p>
     *
     * <p>Complexities:</p>
     *
     * <ul>
     *     <li>append: amortized O(1);</li>
     *     <li>read all entries: O(s), where s is history size;</li>
     *     <li>indexed access: O(1).</li>
     * </ul>
     */
    private final ArrayList<AmbulanceStateChange> stateHistory;

    /**
     * Creates a newly registered ambulance.
     *
     * <p>A newly registered ambulance begins in AVAILABLE because the
     * registration operation represents adding a staffed, operational
     * unit to the dispatch system.</p>
     *
     * <p>The initial registration is not stored as an
     * AmbulanceStateChange because there was no previous state. State
     * history begins with the first actual transition.</p>
     *
     * <p>Time complexity: O(1).</p>
     *
     * <p>Space complexity: O(1), excluding future history entries.</p>
     *
     * @param ambulanceId positive unique ambulance identifier
     * @param name human-readable ambulance name
     * @param initialLocation initial registered location
     * @param registeredAt time the ambulance became available
     *
     * @throws IllegalArgumentException if the ID is not positive or
     *                                  the name is blank
     * @throws NullPointerException if the location or registration
     *                              time is null
     */
    public Ambulance(
            int ambulanceId,
            String name,
            Location initialLocation,
            Instant registeredAt) {

        /*
         * Ambulance IDs serve as HashMap keys and must identify one
         * valid unit. Zero and negative IDs are rejected.
         */
        if (ambulanceId <= 0) {
            throw new IllegalArgumentException(
                    "Ambulance ID must be positive"
            );
        }

        this.ambulanceId = ambulanceId;

        /*
         * Store a normalized, nonblank name.
         */
        this.name = requireText(
                name,
                "Ambulance name"
        );

        /*
         * The ambulance must always have a known location.
         */
        this.location = Objects.requireNonNull(
                initialLocation,
                "Initial ambulance location cannot be null"
        );

        /*
         * Every newly registered ambulance begins as available.
         */
        this.status = AmbulanceStatus.AVAILABLE;

        /*
         * An available ambulance must not already have an assigned call.
         */
        this.activeCall = null;

        /*
         * Registration time is also the first available-since time.
         */
        this.availableSince = Objects.requireNonNull(
                registeredAt,
                "Ambulance registration time cannot be null"
        );

        /*
         * The ArrayList begins empty and grows as transitions occur.
         */
        this.stateHistory = new ArrayList<>();

        /*
         * Verify that the newly created entity satisfies its internal
         * invariants.
         */
        verifyInternalInvariant();
    }

    /**
     * Returns the ambulance's unique identifier.
     *
     * @return positive ambulance ID
     */
    public int getAmbulanceId() {
        return ambulanceId;
    }

    /**
     * Returns the ambulance's display name.
     *
     * @return immutable ambulance name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the ambulance's current immutable location.
     *
     * @return current ambulance location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Returns the ambulance's exact lifecycle status.
     *
     * @return current ambulance status
     */
    public AmbulanceStatus getStatus() {
        return status;
    }

    /**
     * Returns the currently assigned emergency call.
     *
     * <p>The result is null when this ambulance is not in an active-call
     * state.</p>
     *
     * @return active call or null
     */
    public AmbulanceCall getActiveCall() {
        return activeCall;
    }

    /**
     * Returns the time this ambulance most recently became available.
     *
     * @return immutable available-since timestamp
     */
    public Instant getAvailableSince() {
        return availableSince;
    }

    /**
     * Returns whether this ambulance can currently be dispatched.
     *
     * <p>This method delegates the classification to AmbulanceStatus,
     * which is the Information Expert for status categories.</p>
     *
     * @return true only when the status is AVAILABLE
     */
    public boolean isDispatchable() {
        return status.isDispatchable();
    }

    /**
     * Returns whether this ambulance currently has an active emergency
     * assignment.
     *
     * @return true for active-call statuses
     */
    public boolean hasActiveCall() {
        return status.hasActiveCall();
    }

    /**
     * Returns an immutable copy of this ambulance's state history.
     *
     * <p>The internal mutable ArrayList is never returned directly.
     * Returning it directly would allow outside code to add, remove, or
     * reorder history entries.</p>
     *
     * <p>Time complexity: O(s), where s is the number of state-history
     * entries.</p>
     *
     * <p>Space complexity: O(s) for the immutable copy.</p>
     *
     * @return immutable state-history list
     */
    public List<AmbulanceStateChange> getStateHistory() {
        return List.copyOf(stateHistory);
    }

    /**
     * Assigns an available ambulance to an emergency call.
     *
     * <p>Legal transition:</p>
     *
     * <pre>
     * AVAILABLE -> DISPATCHED
     * </pre>
     *
     * <p>The active-call reference is installed before the new status is
     * applied so the resulting DISPATCHED state satisfies the invariant
     * that every active-call status has an active call.</p>
     *
     * <p>Time complexity: amortized O(1). The state-history append is
     * amortized O(1).</p>
     *
     * @param call emergency call being assigned
     * @param assignedAt assignment timestamp
     *
     * @throws NullPointerException if call or time is null
     * @throws IllegalStateException if the ambulance is not AVAILABLE
     *                               or already has an active call
     */
    public void assignTo(
            AmbulanceCall call,
            Instant assignedAt) {

        AmbulanceCall requiredCall = Objects.requireNonNull(
                call,
                "Assigned emergency call cannot be null"
        );

        /*
         * Validate every condition before mutating the entity.
         */
        String reason = validateTransitionRequest(
                AmbulanceStatus.DISPATCHED,
                assignedAt,
                "Assigned to emergency call "
                        + requiredCall.getCallId()
        );

        if (activeCall != null) {
            throw new IllegalStateException(
                    "Ambulance " + ambulanceId
                            + " already has active call "
                            + activeCall.getCallId()
            );
        }

        /*
         * Install the assigned call, then record the legal transition.
         */
        activeCall = requiredCall;

        applyTransition(
                AmbulanceStatus.DISPATCHED,
                assignedAt,
                reason
        );
    }

    /**
     * Marks the ambulance as having arrived at the incident.
     *
     * <p>Legal transition:</p>
     *
     * <pre>
     * DISPATCHED -> ON_SCENE
     * </pre>
     *
     * <p>The ambulance's current location becomes the emergency call's
     * location.</p>
     *
     * @param arrivedAt scene-arrival timestamp
     */
    public void arriveOnScene(Instant arrivedAt) {

        String reason = validateTransitionRequest(
                AmbulanceStatus.ON_SCENE,
                arrivedAt,
                "Arrived at emergency scene"
        );

        AmbulanceCall call = requireActiveCall();

        /*
         * AmbulanceCall and Location are immutable, so sharing the call's
         * Location reference is safe.
         */
        location = call.getLocation();

        applyTransition(
                AmbulanceStatus.ON_SCENE,
                arrivedAt,
                reason
        );
    }

    /**
     * Marks the start of patient transport.
     *
     * <p>Legal transition:</p>
     *
     * <pre>
     * ON_SCENE -> TRANSPORTING
     * </pre>
     *
     * @param startedAt transport-start timestamp
     */
    public void beginTransport(Instant startedAt) {

        String reason = validateTransitionRequest(
                AmbulanceStatus.TRANSPORTING,
                startedAt,
                "Began transporting patient"
        );

        requireActiveCall();

        applyTransition(
                AmbulanceStatus.TRANSPORTING,
                startedAt,
                reason
        );
    }

    /**
     * Marks arrival at a hospital.
     *
     * <p>Legal transition:</p>
     *
     * <pre>
     * TRANSPORTING -> AT_HOSPITAL
     * </pre>
     *
     * @param hospitalLocation hospital location
     * @param arrivedAt hospital-arrival timestamp
     */
    public void arriveAtHospital(
            Location hospitalLocation,
            Instant arrivedAt) {

        Location requiredLocation = Objects.requireNonNull(
                hospitalLocation,
                "Hospital location cannot be null"
        );

        String reason = validateTransitionRequest(
                AmbulanceStatus.AT_HOSPITAL,
                arrivedAt,
                "Arrived at hospital"
        );

        requireActiveCall();

        location = requiredLocation;

        applyTransition(
                AmbulanceStatus.AT_HOSPITAL,
                arrivedAt,
                reason
        );
    }

    /**
     * Completes a transported-patient assignment.
     *
     * <p>Legal transition:</p>
     *
     * <pre>
     * AT_HOSPITAL -> CLEANING_AND_RESTOCKING
     * </pre>
     *
     * <p>The active call is cleared because the patient assignment has
     * ended. The ambulance does not become AVAILABLE immediately.</p>
     *
     * @param completedAt completion timestamp
     * @return the emergency call that was completed
     */
    public AmbulanceCall completeAtHospital(
            Instant completedAt) {

        String reason = validateTransitionRequest(
                AmbulanceStatus.CLEANING_AND_RESTOCKING,
                completedAt,
                "Patient handoff completed; cleaning and restocking required"
        );

        AmbulanceCall completedCall = requireActiveCall();

        /*
         * The target state does not permit an active call.
         */
        activeCall = null;

        applyTransition(
                AmbulanceStatus.CLEANING_AND_RESTOCKING,
                completedAt,
                reason
        );

        return completedCall;
    }

    /**
     * Completes an assignment where no hospital transport is required.
     *
     * <p>Legal transition:</p>
     *
     * <pre>
     * ON_SCENE -> RETURNING_TO_SERVICE
     * </pre>
     *
     * @param completedAt completion timestamp
     * @return the emergency call that was completed
     */
    public AmbulanceCall completeWithoutTransport(
            Instant completedAt) {

        String reason = validateTransitionRequest(
                AmbulanceStatus.RETURNING_TO_SERVICE,
                completedAt,
                "Patient treated on scene; returning to service"
        );

        AmbulanceCall completedCall = requireActiveCall();

        activeCall = null;

        applyTransition(
                AmbulanceStatus.RETURNING_TO_SERVICE,
                completedAt,
                reason
        );

        return completedCall;
    }

    /**
     * Sends an available ambulance to maintenance.
     *
     * <p>Legal transition:</p>
     *
     * <pre>
     * AVAILABLE -> MAINTENANCE
     * </pre>
     *
     * @param changedAt transition timestamp
     * @param reason maintenance reason
     */
    public void sendToMaintenance(
            Instant changedAt,
            String reason) {

        moveAvailableUnitOutOfService(
                AmbulanceStatus.MAINTENANCE,
                changedAt,
                reason
        );
    }

    /**
     * Marks an available ambulance as unstaffed.
     *
     * <p>Legal transition:</p>
     *
     * <pre>
     * AVAILABLE -> UNSTAFFED
     * </pre>
     *
     * @param changedAt transition timestamp
     * @param reason staffing explanation
     */
    public void markUnstaffed(
            Instant changedAt,
            String reason) {

        moveAvailableUnitOutOfService(
                AmbulanceStatus.UNSTAFFED,
                changedAt,
                reason
        );
    }

    /**
     * Administratively removes an available ambulance from service.
     *
     * <p>Legal transition:</p>
     *
     * <pre>
     * AVAILABLE -> OUT_OF_SERVICE
     * </pre>
     *
     * @param changedAt transition timestamp
     * @param reason removal explanation
     */
    public void takeOutOfService(
            Instant changedAt,
            String reason) {

        moveAvailableUnitOutOfService(
                AmbulanceStatus.OUT_OF_SERVICE,
                changedAt,
                reason
        );
    }

    /**
     * Returns a non-active ambulance to AVAILABLE.
     *
     * <p>Valid source states include:</p>
     *
     * <ul>
     *     <li>RETURNING_TO_SERVICE;</li>
     *     <li>CLEANING_AND_RESTOCKING;</li>
     *     <li>MAINTENANCE;</li>
     *     <li>UNSTAFFED;</li>
     *     <li>OUT_OF_SERVICE.</li>
     * </ul>
     *
     * <p>The availableSince value is updated only when the transition
     * to AVAILABLE succeeds.</p>
     *
     * @param serviceLocation location where the ambulance becomes ready
     * @param changedAt transition timestamp
     * @param reason explanation for returning to service
     */
    public void returnToService(
            Location serviceLocation,
            Instant changedAt,
            String reason) {

        Location requiredLocation = Objects.requireNonNull(
                serviceLocation,
                "Return-to-service location cannot be null"
        );

        String normalizedReason = validateTransitionRequest(
                AmbulanceStatus.AVAILABLE,
                changedAt,
                reason
        );

        if (activeCall != null) {
            throw new IllegalStateException(
                    "Ambulance " + ambulanceId
                            + " cannot return to service while assigned to call "
                            + activeCall.getCallId()
            );
        }

        location = requiredLocation;

        applyTransition(
                AmbulanceStatus.AVAILABLE,
                changedAt,
                normalizedReason
        );
    }

    /**
     * Creates an immutable lightweight snapshot of the ambulance.
     *
     * <p>This snapshot can safely leave a synchronized section because
     * it contains no mutable Ambulance reference.</p>
     *
     * <p>The complete state history is intentionally excluded. It is
     * retrieved through a separate operation when needed.</p>
     *
     * @return immutable ambulance snapshot
     */
    public Snapshot snapshot() {

        Long activeCallId = activeCall == null
                ? null
                : activeCall.getCallId();

        return new Snapshot(
                ambulanceId,
                name,
                location,
                status,
                activeCallId,
                availableSince
        );
    }

    /**
     * Moves an AVAILABLE ambulance into one of the non-dispatchable
     * operational states.
     *
     * @param targetStatus MAINTENANCE, UNSTAFFED, or OUT_OF_SERVICE
     * @param changedAt transition timestamp
     * @param reason transition explanation
     */
    private void moveAvailableUnitOutOfService(
            AmbulanceStatus targetStatus,
            Instant changedAt,
            String reason) {

        /*
         * Prevent this helper from being used with an unrelated target.
         */
        if (targetStatus != AmbulanceStatus.MAINTENANCE
                && targetStatus != AmbulanceStatus.UNSTAFFED
                && targetStatus != AmbulanceStatus.OUT_OF_SERVICE) {

            throw new IllegalArgumentException(
                    "Unsupported operational target status: "
                            + targetStatus
            );
        }

        String normalizedReason = validateTransitionRequest(
                targetStatus,
                changedAt,
                reason
        );

        if (activeCall != null) {
            throw new IllegalStateException(
                    "An ambulance with an active call cannot be moved to "
                            + targetStatus
            );
        }

        applyTransition(
                targetStatus,
                changedAt,
                normalizedReason
        );
    }

    /**
     * Validates a requested transition before any state is mutated.
     *
     * @param targetStatus requested destination state
     * @param changedAt transition timestamp
     * @param reason transition reason
     * @return normalized nonblank reason
     */
    private String validateTransitionRequest(
            AmbulanceStatus targetStatus,
            Instant changedAt,
            String reason) {

        Objects.requireNonNull(
                targetStatus,
                "Target ambulance status cannot be null"
        );

        Objects.requireNonNull(
                changedAt,
                "Transition time cannot be null"
        );

        ensureTransitionAllowed(targetStatus);

        return requireText(
                reason,
                "State-transition reason"
        );
    }

    /**
     * Applies a transition that has already been validated.
     *
     * <p>This method updates the status, appends the history entry,
     * updates availableSince when appropriate, and verifies the
     * ambulance's internal invariant.</p>
     */
    private void applyTransition(
            AmbulanceStatus targetStatus,
            Instant changedAt,
            String normalizedReason) {

        AmbulanceStatus previousStatus = status;

        status = targetStatus;

        /*
         * availableSince changes only when entering AVAILABLE.
         */
        if (targetStatus == AmbulanceStatus.AVAILABLE) {
            availableSince = changedAt;
        }

        stateHistory.add(
                new AmbulanceStateChange(
                        previousStatus,
                        targetStatus,
                        changedAt,
                        normalizedReason
                )
        );

        verifyInternalInvariant();
    }

    /**
     * Rejects an illegal transition.
     *
     * <p>EnumMap and EnumSet make this check O(1).</p>
     */
    private void ensureTransitionAllowed(
            AmbulanceStatus targetStatus) {

        EnumSet<AmbulanceStatus> allowedTargets =
                LEGAL_TRANSITIONS.get(status);

        if (!allowedTargets.contains(targetStatus)) {
            throw new IllegalStateException(
                    "Illegal ambulance transition for unit "
                            + ambulanceId
                            + ": "
                            + status
                            + " -> "
                            + targetStatus
            );
        }
    }

    /**
     * Returns the active call or throws when the ambulance should have
     * one but does not.
     */
    private AmbulanceCall requireActiveCall() {

        if (activeCall == null) {
            throw new IllegalStateException(
                    "Ambulance " + ambulanceId
                            + " is in status "
                            + status
                            + " but has no active call"
            );
        }

        return activeCall;
    }

    /**
     * Verifies the relationship between status and activeCall.
     *
     * <p>This is a constant-time local invariant check. It remains
     * enabled in production because it does not scan any collection.</p>
     */
    private void verifyInternalInvariant() {

        if (status.hasActiveCall() && activeCall == null) {
            throw new IllegalStateException(
                    "Active-call status "
                            + status
                            + " requires an assigned call"
            );
        }

        if (!status.hasActiveCall() && activeCall != null) {
            throw new IllegalStateException(
                    "Non-active status "
                            + status
                            + " cannot retain call "
                            + activeCall.getCallId()
            );
        }

        if (location == null) {
            throw new IllegalStateException(
                    "Ambulance location cannot be null"
            );
        }

        if (availableSince == null) {
            throw new IllegalStateException(
                    "Ambulance availableSince cannot be null"
            );
        }
    }

    /**
     * Creates the legal-transition table.
     *
     * @return fully initialized EnumMap
     */
    private static EnumMap<
            AmbulanceStatus,
            EnumSet<AmbulanceStatus>
            > createLegalTransitions() {

        EnumMap<
                AmbulanceStatus,
                EnumSet<AmbulanceStatus>
                > transitions =
                new EnumMap<>(AmbulanceStatus.class);

        /*
         * Add an empty destination set for every state so map lookups
         * never return null.
         */
        for (AmbulanceStatus status : AmbulanceStatus.values()) {
            transitions.put(
                    status,
                    EnumSet.noneOf(AmbulanceStatus.class)
            );
        }

        transitions.get(AmbulanceStatus.AVAILABLE).addAll(
                EnumSet.of(
                        AmbulanceStatus.DISPATCHED,
                        AmbulanceStatus.MAINTENANCE,
                        AmbulanceStatus.UNSTAFFED,
                        AmbulanceStatus.OUT_OF_SERVICE
                )
        );

        transitions.get(AmbulanceStatus.DISPATCHED).add(
                AmbulanceStatus.ON_SCENE
        );

        transitions.get(AmbulanceStatus.ON_SCENE).addAll(
                EnumSet.of(
                        AmbulanceStatus.TRANSPORTING,
                        AmbulanceStatus.RETURNING_TO_SERVICE
                )
        );

        transitions.get(AmbulanceStatus.TRANSPORTING).add(
                AmbulanceStatus.AT_HOSPITAL
        );

        transitions.get(AmbulanceStatus.AT_HOSPITAL).add(
                AmbulanceStatus.CLEANING_AND_RESTOCKING
        );

        transitions.get(
                AmbulanceStatus.RETURNING_TO_SERVICE
        ).add(AmbulanceStatus.AVAILABLE);

        transitions.get(
                AmbulanceStatus.CLEANING_AND_RESTOCKING
        ).add(AmbulanceStatus.AVAILABLE);

        transitions.get(
                AmbulanceStatus.MAINTENANCE
        ).add(AmbulanceStatus.AVAILABLE);

        transitions.get(
                AmbulanceStatus.UNSTAFFED
        ).add(AmbulanceStatus.AVAILABLE);

        transitions.get(
                AmbulanceStatus.OUT_OF_SERVICE
        ).add(AmbulanceStatus.AVAILABLE);

        return transitions;
    }

    /**
     * Validates required String values.
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
     * Returns a readable representation for console output and
     * debugging.
     */
    @Override
    public String toString() {
        return "Ambulance{"
                + "ambulanceId=" + ambulanceId
                + ", name='" + name + '\''
                + ", location=" + location
                + ", status=" + status
                + ", activeCallId="
                + (activeCall == null
                        ? null
                        : activeCall.getCallId())
                + ", availableSince=" + availableSince
                + '}';
    }

    /**
     * Immutable read-only representation of an Ambulance.
     *
     * <p>The snapshot is safe to pass outside a synchronized operation
     * because it does not contain a mutable Ambulance reference.</p>
     *
     * @param ambulanceId unique ambulance identifier
     * @param name display name
     * @param location immutable current location
     * @param status current lifecycle status
     * @param activeCallId active call ID or null
     * @param availableSince most recent AVAILABLE timestamp
     */
    public record Snapshot(
            int ambulanceId,
            String name,
            Location location,
            AmbulanceStatus status,
            Long activeCallId,
            Instant availableSince) {

        /**
         * Validates every snapshot created by Ambulance.snapshot().
         */
        public Snapshot {

            if (ambulanceId <= 0) {
                throw new IllegalArgumentException(
                        "Snapshot ambulance ID must be positive"
                );
            }

            name = requireText(
                    name,
                    "Snapshot ambulance name"
            );

            Objects.requireNonNull(
                    location,
                    "Snapshot location cannot be null"
            );

            Objects.requireNonNull(
                    status,
                    "Snapshot status cannot be null"
            );

            Objects.requireNonNull(
                    availableSince,
                    "Snapshot availableSince cannot be null"
            );

            /*
             * A snapshot must preserve the same state/call invariant
             * as the mutable Ambulance entity.
             */
            if (status.hasActiveCall() && activeCallId == null) {
                throw new IllegalArgumentException(
                        "Active-call snapshot status requires a call ID"
                );
            }

            if (!status.hasActiveCall() && activeCallId != null) {
                throw new IllegalArgumentException(
                        "Non-active snapshot status cannot contain a call ID"
                );
            }
        }
    }
}

