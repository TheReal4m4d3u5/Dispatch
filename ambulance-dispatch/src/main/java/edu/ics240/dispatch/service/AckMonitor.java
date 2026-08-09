package edu.ics240.dispatch.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

import org.springframework.stereotype.Service;
import java.util.function.LongConsumer;
/**
 * STEP 9: monitors acknowledgement timers.
 * If timer expires, calls crewNotResponding(dispatchId).
 */
@Service
public class AckMonitor {

 
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private final Map<Long, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();



    public void armAckTimer(long dispatchId, int seconds, LongConsumer onTimeout) {
        cancelAckTimer(dispatchId);
        ScheduledFuture<?> future = scheduler.schedule(
                () -> onTimeout.accept(dispatchId),   // was: notificationService.crewNotResponding(dispatchId)
                seconds,
                TimeUnit.SECONDS
        );
        timers.put(dispatchId, future);
    }

    public void cancelAckTimer(long dispatchId) {
        ScheduledFuture<?> future = timers.remove(dispatchId);
        if (future != null) {
            future.cancel(false);
        }
    }

	public void cancel(long id) {
		// TODO Auto-generated method stub
		
	}

	public void arm(long dispatchId) {
		// TODO Auto-generated method stub
		
	}
}
