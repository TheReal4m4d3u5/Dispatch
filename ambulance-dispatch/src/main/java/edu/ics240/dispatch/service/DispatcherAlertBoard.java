package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.DispatcherAlert;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Where post-commit failures surface. A dispatch that committed but never reached a crew
 * is the one failure in ED-01 nobody would otherwise notice.
 */
@Component
public class DispatcherAlertBoard {

    private final List<DispatcherAlert> alerts = new CopyOnWriteArrayList<>();
    private final Map<Long, Boolean> raised = new ConcurrentHashMap<>();

    public void raise(long dispatchId, DispatcherAlert.Kind kind, String message, Instant at) {
        if (raised.putIfAbsent(key(dispatchId, kind), Boolean.TRUE) == null) {
            alerts.add(new DispatcherAlert(dispatchId, kind, message, at));
        }
    }

    public void clear(long dispatchId) {
        alerts.removeIf(alert -> alert.dispatchId() == dispatchId);
        for (DispatcherAlert.Kind kind : DispatcherAlert.Kind.values()) {
            raised.remove(key(dispatchId, kind));
        }
    }

    public List<DispatcherAlert> current() {
        return List.copyOf(alerts);
    }

    public boolean hasAlertFor(long dispatchId) {
        return alerts.stream().anyMatch(alert -> alert.dispatchId() == dispatchId);
    }

    private long key(long dispatchId, DispatcherAlert.Kind kind) {
        return dispatchId * 31L + kind.ordinal();
    }
}