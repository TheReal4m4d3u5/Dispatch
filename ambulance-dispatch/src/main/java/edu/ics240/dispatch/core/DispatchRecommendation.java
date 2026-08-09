package edu.ics240.dispatch.core;
 
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
 
/**
 * Entity representing a ranked recommendation for a call.
 * Holds the recommended ambulance, alternatives, and selection.
 */
public class DispatchRecommendation {
 
    /** Assumed average urban response speed, km/h, for the displayed travel estimate. */
    private static final double AVERAGE_SPEED_KMH = 40.0;
 
    private final long id;
    private final AmbulanceCall call;
    private final Ambulance recommendedAmbulance;
    private final List<Ambulance> rankedAlternatives;
    private final Instant createdAt;
    private final Instant leaseExpiresAt;
 
    private Ambulance selectedAmbulance;
    private SelectionSource selectionSource;
    private OverrideReason reasonCode;
 
    public DispatchRecommendation(long id,
                                  AmbulanceCall call,
                                  Ambulance recommendedAmbulance,
                                  List<Ambulance> rankedAlternatives,
                                  Instant leaseExpiresAt) {
        this.id = id;
        this.call = call;
        this.recommendedAmbulance = recommendedAmbulance;
        this.rankedAlternatives = rankedAlternatives;
        this.createdAt = Instant.now();
        this.leaseExpiresAt = leaseExpiresAt;
    }
 
    // ---------------------------------------------------------------- accessors
 
    public long getId() {
        return id;
    }
 
    /** Alias for {@link #getId()}; the BDD harness reads this name. */
    public long getRecommendationId() {
        return id;
    }
 
    public AmbulanceCall getCall() {
        return call;
    }
 
    public Ambulance getRecommendedAmbulance() {
        return recommendedAmbulance;
    }
 
    public List<Ambulance> getRankedAlternatives() {
        return rankedAlternatives == null ? List.of() : rankedAlternatives;
    }
 
    /** Every unit considered, best first: the recommended one followed by its alternatives. */
    public List<Ambulance> getRankedCandidates() {
        List<Ambulance> all = new ArrayList<>();
        if (recommendedAmbulance != null) {
            all.add(recommendedAmbulance);
        }
        if (rankedAlternatives != null) {
            all.addAll(rankedAlternatives);
        }
        return List.copyOf(all);
    }
 
    public Ambulance getSelectedAmbulance() {
        return selectedAmbulance;
    }
 
    public SelectionSource getSelectionSource() {
        return selectionSource;
    }
 
    public OverrideReason getReasonCode() {
        return reasonCode;
    }
 
    /** Alias for {@link #getReasonCode()}; the BDD harness reads this name. */
    public OverrideReason getOverrideReason() {
        return reasonCode;
    }
 
    public Instant getCreatedAt() {
        return createdAt;
    }
 
    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }
 
    /**
     * Estimated travel time for the recommended unit to reach the call.
     * Straight-line distance at an assumed average speed; null when either
     * location is unknown.
     */
    public Duration getTravelEstimate() {
        if (recommendedAmbulance == null || call == null
                || recommendedAmbulance.getLocation() == null
                || call.getLocation() == null) {
            return null;
        }
        double distanceKm = recommendedAmbulance.getLocation().distanceTo(call.getLocation());
        double minutes = (distanceKm / AVERAGE_SPEED_KMH) * 60.0;
        long seconds = Math.round(minutes * 60.0);
        return Duration.ofSeconds(Math.max(60L, seconds));   // never report under a minute
    }
 
    // ---------------------------------------------------------------- behaviour
 
    /**
     * Step 2 & Step 6: lease expiry check.
     * If expired, recommendation is no longer valid.
     */
    public boolean isLeaseExpired() {
        return Instant.now().isAfter(leaseExpiresAt);
    }
 
    /**
     * Step 5: record dispatcher selection.
     * Guards: ambulance must be in eligible set.
     */
    public void select(Ambulance ambulance,
                       SelectionSource source,
                       OverrideReason overrideReason) {
        boolean inEligibleSet = ambulance.getId() == recommendedAmbulance.getId()
                || getRankedAlternatives().stream().anyMatch(a -> a.getId() == ambulance.getId());
        if (!inEligibleSet) {
            throw new IllegalArgumentException("Ambulance not in eligible set.");
        }
        this.selectedAmbulance = ambulance;
        this.selectionSource = source;
        this.reasonCode = overrideReason;
    }
 
    @Override
    public String toString() {
        return "DispatchRecommendation{" +
                "id=" + id +
                ", call=" + (call == null ? null : call.getId()) +
                ", recommended=" + (recommendedAmbulance == null ? null : recommendedAmbulance.getCallSign()) +
                ", alternatives=" + getRankedAlternatives().size() +
                ", selected=" + (selectedAmbulance == null ? null : selectedAmbulance.getCallSign()) +
                '}';
    }
}