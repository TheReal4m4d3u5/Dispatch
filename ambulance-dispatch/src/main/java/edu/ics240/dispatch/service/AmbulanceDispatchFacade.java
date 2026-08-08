package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.*;

import java.util.List;

public class AmbulanceDispatchFacade {

    private final AmbulanceCallCenter
            callCenter;

    private final CadRecommendationService
            cadService;

    public AmbulanceDispatchFacade(
            AmbulanceCallCenter callCenter,
            CadRecommendationService cadService) {

        this.callCenter = callCenter;
        this.cadService = cadService;
    }

    public DispatchRecommendation
            recommendNext() {

        AmbulanceCall call =
                callCenter.getNextWaitingCall();

        if (call == null) {
            throw new IllegalStateException(
                    "No emergency calls are waiting."
            );
        }

        List<Ambulance> available =
                callCenter
                        .getAvailableAmbulances();

        Ambulance best =
                cadService.recommend(
                        call,
                        available
                );

        if (best == null) {
            throw new IllegalStateException(
                    "No appropriate ambulance is available."
            );
        }

        return callCenter
                .createRecommendation(
                        call,
                        best
                );
    }

    public DispatchRecord approveRecommendation(
            long recommendationId) {

        return callCenter.confirmDispatch(
                recommendationId
        );
    }
}