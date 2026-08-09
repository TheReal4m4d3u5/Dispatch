package edu.ics240.dispatch.web;

import edu.ics240.dispatch.core.DispatchRecord;
import edu.ics240.dispatch.core.ValidationStatus;
import edu.ics240.dispatch.service.AmbulanceDispatchFacade;
import org.springframework.web.bind.annotation.*;

/**
 * STEP 5 & 6 & 7 endpoints: select + validate + confirm/cancel.
 */
@RestController
@RequestMapping("/api/selection")
public class SelectionController {

    private final AmbulanceDispatchFacade facade;

    public SelectionController(AmbulanceDispatchFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/{recommendationId}/accept")
    public void acceptRecommended(@PathVariable long recommendationId) {
        facade.selectRecommendedAmbulance(recommendationId);
    }

    @PostMapping("/{recommendationId}/override")
    public void overrideSelection(@PathVariable long recommendationId,
                                  @RequestBody OverrideDto dto) {
        facade.selectAlternateAmbulance(recommendationId, dto.ambulanceId(), dto.reasonCode());
    }

    @PostMapping("/{recommendationId}/validate")
    public ValidationStatus validate(@PathVariable long recommendationId) {
        return ValidationStatus.OK;
    }

    @PostMapping("/{recommendationId}/confirm")
    public DispatchRecord confirm(@PathVariable long recommendationId) {
        return facade.confirmDispatch(recommendationId);
    }

    @PostMapping("/{recommendationId}/cancel")
    public void cancel(@PathVariable long recommendationId) {
        facade.cancelRecommendation(recommendationId);
    }

    public record OverrideDto(long ambulanceId, String reasonCode) {}
}
