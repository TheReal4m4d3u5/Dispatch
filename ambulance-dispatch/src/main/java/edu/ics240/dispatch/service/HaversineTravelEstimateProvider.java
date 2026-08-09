package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.Location;

import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Offline stand-in for a real routing service. Deliberately crude: the point is that
 * ranking depends on the TravelEstimateProvider interface rather than on
 * Location.distanceTo(), so swapping in OSRM or a commercial matrix API is a one-class
 * change with no effect on the domain.
 */
@Component
public class HaversineTravelEstimateProvider implements TravelEstimateProvider {

    private static final double WINDING_FACTOR = 1.35;
    private static final double AVERAGE_SPEED_KMH = 55.0;
    private static final Duration BASE_OVERHEAD = Duration.ofSeconds(45);

    @Override
    public TravelEstimate estimate(Location origin, Location destination) {
        double roadKm = origin.distanceTo(destination) * WINDING_FACTOR;
        long travelSeconds = Math.round(roadKm / AVERAGE_SPEED_KMH * 3600.0);
        long totalSeconds = travelSeconds + BASE_OVERHEAD.getSeconds();
        int etaMinutes = (int) Math.max(1, Math.round(totalSeconds / 60.0));
        return new TravelEstimate(roadKm, etaMinutes);
    }
}