package edu.ics240.dispatch.bdd;

import edu.ics240.dispatch.core.Ambulance;
import edu.ics240.dispatch.core.AmbulanceCrew;
import edu.ics240.dispatch.core.CrewAssignment;
import edu.ics240.dispatch.core.CrewDeviceGateway;
import edu.ics240.dispatch.core.DeliveryResult;
import edu.ics240.dispatch.core.DispatchRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Gateway that can be told to fail, so the delivery-failure scenarios are reachable.
 *
 * <p>Deliberately not a Spring bean: DispatchWorld holds it directly, so a test can flip
 * reachability mid-scenario without touching the container.
 */
public class RecordingCrewDeviceGateway implements CrewDeviceGateway {

    private final List<Long> delivered = new ArrayList<>();
    private boolean reachable = true;
    private int attempts;

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public int getAttempts() {
        return attempts;
    }

    public List<Long> getDelivered() {
        return delivered;
    }

    /** The one CrewNotificationService actually calls. */
    @Override
    public DeliveryResult sendDispatch(DispatchRecord record) {
        attempts++;
        if (!reachable) {
            return DeliveryResult.unreachable("MDT");
        }
        delivered.add(record.getId());
        return DeliveryResult.delivered("MDT");
    }

    @Override
    public DeliveryResult send(AmbulanceCrew crew, CrewAssignment assignment, long dispatchId) {
        attempts++;
        if (crew == null) {
            return DeliveryResult.noCrew();
        }
        if (!reachable) {
            return DeliveryResult.unreachable(crew.getDeviceId());
        }
        delivered.add(dispatchId);
        return DeliveryResult.delivered(crew.getDeviceId());
    }

    @Override
    public DeliveryResult sendAssignment(Ambulance ambulance, CrewAssignment assignment, long dispatchId) {
        attempts++;
        if (!reachable) {
            return DeliveryResult.unreachable("MDT");
        }
        delivered.add(dispatchId);
        return DeliveryResult.delivered("MDT");
    }

    @Override
    public void sendAcknowledgement(DispatchRecord record) {
        // not exercised by the ED-01 scenarios
    }

    @Override
    public void sendInProgress(DispatchRecord record) {
        // not exercised by the ED-01 scenarios
    }
}