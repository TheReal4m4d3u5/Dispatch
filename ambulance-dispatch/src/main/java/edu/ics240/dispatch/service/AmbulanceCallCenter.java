package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.*;

import java.util.*;

public class AmbulanceCallCenter {

    private final PriorityQueue<AmbulanceCall>
            waitingCalls =
            new PriorityQueue<>(
                    new EmergencyCallComparator()
            );

    private final Map<Integer, Ambulance>
            ambulances =
            new HashMap<>();

    private final Map<Long, DispatchRecommendation>
            recommendations =
            new HashMap<>();

    private final Map<Long, DispatchRecord>
            activeDispatches =
            new HashMap<>();

    private long nextRecommendationId = 1;
    private long nextDispatchId = 1;

    public void addCall(AmbulanceCall call) {
        waitingCalls.add(call);
    }

    public void addAmbulance(Ambulance ambulance) {
        ambulances.put(
        		ambulance.getAmbulanceId(),
                ambulance
        );
    }

    public AmbulanceCall getNextWaitingCall() {
        return waitingCalls.peek();
    }

    public List<Ambulance> getAvailableAmbulances() {

        List<Ambulance> available =
                new ArrayList<>();

        for (Ambulance ambulance : ambulances.values()) {

            if (ambulance.isAvailable()) {
                available.add(ambulance);
            }
        }

        return available;
    }

    public DispatchRecommendation
            createRecommendation(
                    AmbulanceCall call,
                    Ambulance ambulance) {

        DispatchRecommendation recommendation =
                new DispatchRecommendation(
                        nextRecommendationId++,
                        call,
                        ambulance
                );

        recommendations.put(
                recommendation.getRecommendationId(),
                recommendation
        );

        return recommendation;
    }

    public DispatchRecommendation
            getRecommendation(
                    long recommendationId) {

        return recommendations.get(
                recommendationId
        );
    }

    public DispatchRecord confirmDispatch(
            long recommendationId) {

        DispatchRecommendation recommendation =
                recommendations.get(
                        recommendationId
                );

        if (recommendation == null) {
            throw new IllegalArgumentException(
                    "Recommendation does not exist."
            );
        }

        Ambulance ambulance =
                recommendation
                        .getRecommendedAmbulance();

        AmbulanceCall call =
                recommendation.getCall();

        /*
         * STEP 6:
         * Revalidate immediately before dispatch.
         */
        if (!ambulance.isAvailable()) {
            throw new IllegalStateException(
                    "Selected ambulance is no longer available."
            );
        }

        if (!waitingCalls.contains(call)) {
            throw new IllegalStateException(
                    "Emergency call no longer requires dispatch."
            );
        }

        /*
         * Now commit.
         */
        ambulance.assignTo(call);

        waitingCalls.remove(call);

        DispatchRecord dispatch =
                new DispatchRecord(
                        nextDispatchId++,
                        call,
                        ambulance
                );

        activeDispatches.put(
                dispatch.getDispatchId(),
                dispatch
        );

        recommendations.remove(
                recommendationId
        );

        return dispatch;
    }
}