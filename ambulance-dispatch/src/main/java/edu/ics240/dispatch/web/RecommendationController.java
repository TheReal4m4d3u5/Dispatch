package edu.ics240.dispatch.web;

import edu.ics240.dispatch.core.DispatchRecommendation;
import edu.ics240.dispatch.service.AmbulanceDispatchFacade;
import org.springframework.web.bind.annotation.*;

/**
 * STEP 2 & 4 endpoints: recommendNext + view recommendation.
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final AmbulanceDispatchFacade facade;

    public RecommendationController(AmbulanceDispatchFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/next")
    public DispatchRecommendation recommendNext(@RequestParam long dispatcherId) {
        return facade.recommendNext(dispatcherId, 120); // 2-minute lease
    }

    @GetMapping("/{recommendationId}")
    public RecommendationView getView(@PathVariable long recommendationId) {
        return facade.getRecommendationView(recommendationId);
    }

    public record RecommendationView(
            long recommendationId,
            long callId,
            long recommendedAmbulanceId,
            java.util.List<Long> alternativeAmbulanceIds
    ) {}
}
