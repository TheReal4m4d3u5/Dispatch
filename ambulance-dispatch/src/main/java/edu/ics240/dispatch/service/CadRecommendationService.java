package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.*;

import java.util.List;

public class CadRecommendationService {

    public Ambulance recommend(
            AmbulanceCall call,
            List<Ambulance> ambulances) {

        Ambulance best = null;
        double bestDistance =
                Double.MAX_VALUE;

        for (Ambulance ambulance :
                ambulances) {

            if (!ambulance.isAvailable()) {
                continue;
            }

            if (!ambulance.isAppropriateFor(call)) {
                continue;
            }

            double distance =
                    ambulance
                            .getLocation()
                            .distanceTo(
                                    call.getLocation()
                            );

            if (distance < bestDistance) {

                best = ambulance;
                bestDistance = distance;
            }
        }

        return best;
    }
}