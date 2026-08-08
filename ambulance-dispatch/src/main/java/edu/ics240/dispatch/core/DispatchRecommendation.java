package edu.ics240.dispatch.core;

public class DispatchRecommendation {

    private final long recommendationId;
    private final AmbulanceCall call;
    private final Ambulance recommendedAmbulance;

    public DispatchRecommendation(
            long recommendationId,
            AmbulanceCall call,
            Ambulance recommendedAmbulance) {

        this.recommendationId = recommendationId;
        this.call = call;
        this.recommendedAmbulance =
                recommendedAmbulance;
    }

    public long getRecommendationId() {
        return recommendationId;
    }

    public AmbulanceCall getCall() {
        return call;
    }

    public Ambulance getRecommendedAmbulance() {
        return recommendedAmbulance;
    }
}