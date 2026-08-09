package edu.ics240.dispatch.web;

import edu.ics240.dispatch.service.AmbulanceCallCenter;
import edu.ics240.dispatch.service.AckMonitor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * STEP 9 endpoints: crew acknowledges and begins response.
 */
@RestController
@RequestMapping("/api/crew")
public class CrewController {

    private final AmbulanceCallCenter callCenter;
    private final AckMonitor ackMonitor;

    public CrewController(AmbulanceCallCenter callCenter,
                          AckMonitor ackMonitor) {
        this.callCenter = callCenter;
        this.ackMonitor = ackMonitor;
    }

    @PostMapping("/dispatch/{dispatchId}/acknowledge")
    public void acknowledge(@PathVariable long dispatchId) {
        callCenter.acknowledgeDispatch(dispatchId, Instant.now());
        ackMonitor.cancelAckTimer(dispatchId);
    }

    @PostMapping("/dispatch/{dispatchId}/begin-response")
    public void beginResponse(@PathVariable long dispatchId) {
        callCenter.beginResponse(dispatchId);
    }
}
