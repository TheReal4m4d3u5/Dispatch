package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.CrewDeviceGateway;
import edu.ics240.dispatch.core.DeliveryResult;
import edu.ics240.dispatch.core.DispatchOutbox;
import edu.ics240.dispatch.core.DispatchRecord;
import edu.ics240.dispatch.core.DispatcherAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * STEP 8. Sends the emergency assignment to the selected Ambulance Crew.
 *
 * <p>Runs after the dispatch has committed, driven by the outbox, so a coverage gap
 * cannot roll back a valid dispatch and a committed dispatch cannot vanish unnoticed.
 * Every attempt returns a DeliveryResult, retries are bounded and deduplicated on
 * dispatch id, and a final failure raises a dispatcher alert rather than disappearing.
 */
@Service
public class CrewNotificationService {

    private static final Logger log = LoggerFactory.getLogger(CrewNotificationService.class);

    private final DispatchOutbox outbox;
    private final CrewDeviceGateway gateway;
    private final AckMonitor ackMonitor;
    private final DispatcherAlertBoard alertBoard;
    private final Clock clock;
    private final int maxAttempts;

    public CrewNotificationService(DispatchOutbox outbox,
                                   CrewDeviceGateway gateway,
                                   AckMonitor ackMonitor,
                                   DispatcherAlertBoard alertBoard,
                                   Clock clock,
                                   @Value("${dispatch.max-delivery-attempts:3}") int maxAttempts) {
        this.outbox = Objects.requireNonNull(outbox, "outbox");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.ackMonitor = Objects.requireNonNull(ackMonitor, "ackMonitor");
        this.alertBoard = Objects.requireNonNull(alertBoard, "alertBoard");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.maxAttempts = maxAttempts;
    }

    /** Drains the outbox. Scheduled in production, called directly in tests. */
    @Scheduled(fixedDelayString = "${dispatch.outbox-poll-ms:1000}")
    public void drainOutbox() {
        Optional<DispatchRecord> next = outbox.claimNext();
        while (next.isPresent()) {
            deliver(next.get());
            next = outbox.claimNext();
        }
    }

    /**
     * One delivery attempt for one dispatch.
     *
     * @return the result, so callers and tests can branch on it
     */
    public DeliveryResult deliver(DispatchRecord record) {
        Objects.requireNonNull(record, "record");
        long dispatchId = record.getId();
        Instant now = clock.instant();

        outbox.recordAttempt(dispatchId, now);
        DeliveryResult result = gateway.sendDispatch(record);

        if (result.isDelivered()) {
            outbox.markDelivered(dispatchId);
            record.markAssigned(now);
            ackMonitor.arm(dispatchId);
            return result;
        }

        if (outbox.attemptsFor(dispatchId) < maxAttempts) {
            log.warn("dispatch {} delivery attempt {} failed", dispatchId, outbox.attemptsFor(dispatchId));
            outbox.requeue(dispatchId);
            return result;
        }

        log.error("dispatch {} undeliverable after {} attempts", dispatchId, maxAttempts);
        alertBoard.raise(dispatchId,
                DispatcherAlert.Kind.CREW_UNREACHABLE,
                "assignment could not be delivered",
                now);
        return result;
    }

	public Object crewNotResponding(long dispatchId) {
		// TODO Auto-generated method stub
		return null;
	}
}