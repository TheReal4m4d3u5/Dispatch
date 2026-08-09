package edu.ics240.dispatch.core;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Entity representing an ambulance unit.
 * Info Expert for dispatchability and appropriateness checks.
 */
public class Ambulance {

    private final long id;

    private String callSign;
    private AmbulanceStatus status;
    private Location location;
    private Set<String> capabilities;   // e.g. "ALS", "BLS", "PEDIATRIC"
    private String jurisdiction;

    /** When the unit last became available. Drives the idle-time ranking tiebreak. */
    private Instant availableSince;

    private AmbulanceCrew assignedCrew;

    public Ambulance(long id, AmbulanceStatus status, Location location,
                     Set<String> capabilities, String jurisdiction) {
        this.id = id;
        this.status = status;
        this.location = location;
        this.capabilities = capabilities;
        this.jurisdiction = jurisdiction;
    }

    // ---------------------------------------------------------------- accessors

    public long getId() {
        return id;
    }

    public String getCallSign() {
        return callSign;
    }

    public void setCallSign(String callSign) {
        this.callSign = callSign;
    }

    public AmbulanceStatus getStatus() {
        return status;
    }

    public void setStatus(AmbulanceStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Set<String> capabilities) {
        this.capabilities = capabilities;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public Instant getAvailableSince() {
        return availableSince;
    }

    public void setAvailableSince(Instant availableSince) {
        this.availableSince = availableSince;
    }

    public AmbulanceCrew getAssignedCrew() {
        return assignedCrew;
    }

    public void assignCrew(AmbulanceCrew crew) {
        this.assignedCrew = crew;
    }

    // ---------------------------------------------------------------- guards

    /**
     * Step 2 & Step 6 & Step 7:
     * Guard: can this unit be dispatched right now?
     */
    public boolean isDispatchable() {
        return status == AmbulanceStatus.AVAILABLE;
    }

    /**
     * Step 2 & Step 6:
     * Guard: is this unit appropriate for the given call?
     *
     * <p>Null-safe: a unit with no jurisdiction or no capabilities is never appropriate,
     * rather than throwing.
     */
    public boolean isAppropriateFor(AmbulanceCall call) {
        if (call == null) {
            return false;
        }
        if (jurisdiction == null || !jurisdiction.equals(call.getJurisdiction())) {
            return false;
        }
        String required = call.getRequiredCapability();
        if (required != null) {
            return capabilities != null && capabilities.contains(required);
        }
        return true;
    }

    /**
     * How long this unit has been idle as of {@code now}. Zero when unknown,
     * so a missing timestamp sorts last rather than blowing up the comparator.
     */
    public java.time.Duration idleTimeAsOf(Instant now) {
        if (availableSince == null || now == null) {
            return java.time.Duration.ZERO;
        }
        return java.time.Duration.between(availableSince, now);
    }

    // ---------------------------------------------------------------- transitions

    /** Step 7: mark unit as dispatched. */
    public void markDispatched() {
        this.status = AmbulanceStatus.DISPATCHED;
    }

    /** Step 9: crew begins responding, unit is en route. */
    public void markEnRoute() {
        this.status = AmbulanceStatus.EN_ROUTE;
    }

    /** Unit returns to the available pool as of {@code now}. */
    public void markAvailable(Instant now) {
        this.status = AmbulanceStatus.AVAILABLE;
        this.availableSince = now;
    }

    // ---------------------------------------------------------------- utility

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ambulance)) return false;
        return id == ((Ambulance) o).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Ambulance{" +
                "id=" + id +
                ", callSign='" + callSign + '\'' +
                ", status=" + status +
                ", jurisdiction='" + jurisdiction + '\'' +
                ", capabilities=" + capabilities +
                ", availableSince=" + availableSince +
                '}';
    }
}
