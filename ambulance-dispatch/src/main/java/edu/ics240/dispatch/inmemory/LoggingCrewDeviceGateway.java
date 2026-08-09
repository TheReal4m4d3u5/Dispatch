package edu.ics240.dispatch.inmemory;

import edu.ics240.dispatch.core.Ambulance;
import edu.ics240.dispatch.core.AmbulanceCrew;
import edu.ics240.dispatch.core.CrewAssignment;
import edu.ics240.dispatch.core.CrewDeviceGateway;
import edu.ics240.dispatch.core.DeliveryResult;
import edu.ics240.dispatch.core.DispatchRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Development transport for crew devices. Logs and reports success. */
@Component
public class LoggingCrewDeviceGateway implements CrewDeviceGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingCrewDeviceGateway.class);

    @Override
    public DeliveryResult sendDispatch(DispatchRecord record) {
        log.info("MDT <- dispatch {}", record.getId());
        return DeliveryResult.delivered("MDT");
    }

    @Override
    public void sendAcknowledgement(DispatchRecord record) {
        log.info("dispatch {} acknowledged", record.getId());
    }

    @Override
    public void sendInProgress(DispatchRecord record) {
        log.info("dispatch {} is en route", record.getId());
    }

    @Override
    public DeliveryResult sendAssignment(Ambulance ambulance,
                                         CrewAssignment assignment,
                                         long dispatchId) {
        log.info("MDT <- assignment for dispatch {}", dispatchId);
        return DeliveryResult.delivered("MDT");
    }

    @Override
    public DeliveryResult send(AmbulanceCrew crew, CrewAssignment assignment, long dispatchId) {
        if (crew == null) {
            return DeliveryResult.noCrew();
        }
        log.info("MDT {} <- dispatch {}", crew.getDeviceId(), dispatchId);
        return DeliveryResult.delivered(crew.getDeviceId());
    }
}
