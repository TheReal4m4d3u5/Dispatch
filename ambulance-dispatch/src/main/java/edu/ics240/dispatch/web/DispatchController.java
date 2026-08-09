package edu.ics240.dispatch.web;

import edu.ics240.dispatch.core.*;
import edu.ics240.dispatch.service.AmbulanceCallCenter;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * STEP 1 endpoint: complete evaluation.
 */
@RestController
@RequestMapping("/api/dispatch")
public class DispatchController {

    private final AmbulanceCallCenter callCenter;

    public DispatchController(AmbulanceCallCenter callCenter) {
        this.callCenter = callCenter;
    }

    @PostMapping("/calls/{callId}/evaluation")
    public void completeEvaluation(@PathVariable long callId,
                                   @RequestBody EvaluationDto dto) {
        AmbulanceCall call = callCenter.getCall(callId);
        if (call == null) {
            throw new NoSuchElementException("Call not found: " + callId);
        }
        callCenter.completeEvaluation(
                call,
                dto.priority(),
                dto.requiredCapability(),
                dto.jurisdiction(),
                dto.requiresDispatch()
        );
    }

    public record EvaluationDto(
            Priority priority,
            String requiredCapability,
            String jurisdiction,
            boolean requiresDispatch
    ) {}
}