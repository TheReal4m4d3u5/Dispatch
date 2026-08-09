package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.Location;

/**
 * STEP 3: Service that estimates road distance + ETA.
 * You can plug in real routing later; for now, a heuristic.
 */
public interface TravelEstimateProvider {

    TravelEstimate estimate(Location ambulanceLocation, Location callLocation);

    record TravelEstimate(double roadKm, int etaMinutes) {}
}
