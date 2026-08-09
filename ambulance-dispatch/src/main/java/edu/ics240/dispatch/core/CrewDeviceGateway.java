package edu.ics240.dispatch.core;

import edu.ics240.dispatch.core.DispatchRecord;

/**
 * Abstraction for delivering dispatch notifications to crew devices.
 * Implementations might use SMS, push notifications, radio, etc.
 */
public interface CrewDeviceGateway {

    /**
     * Deliver the dispatch record to the crew device.
     * @return 
     */
    DeliveryResult sendDispatch(DispatchRecord record);

    /**
     * Notify crew that the dispatch was acknowledged.
     */
    void sendAcknowledgement(DispatchRecord record);

    /**
     * Notify crew that the call is now in progress.
     */
    void sendInProgress(DispatchRecord record);
    
    DeliveryResult sendAssignment(Ambulance ambulance,
            CrewAssignment assignment,
            long dispatchId);

	DeliveryResult send(AmbulanceCrew crew, CrewAssignment assignment, long dispatchId);
}
