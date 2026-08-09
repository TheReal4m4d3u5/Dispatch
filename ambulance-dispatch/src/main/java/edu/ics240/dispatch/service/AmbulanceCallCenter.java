package edu.ics240.dispatch.service;

import edu.ics240.dispatch.core.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AmbulanceCallCenter {
	private final Map<Long, AmbulanceCall> calls = new ConcurrentHashMap<>();
    private final WaitingCalls waitingCalls;
    private final AmbulanceCallRepository callRepo;
    private final AmbulanceRepository ambulanceRepo;
    private final DispatchRecommendationRepository recommendationRepo;
    private final DispatchRecordRepository dispatchRecordRepo;

    public AmbulanceCallCenter(WaitingCalls waitingCalls,
                               AmbulanceCallRepository callRepo,
                               AmbulanceRepository ambulanceRepo,
                               DispatchRecommendationRepository recommendationRepo,
                               DispatchRecordRepository dispatchRecordRepo) {
        this.waitingCalls = waitingCalls;
        this.callRepo = callRepo;
        this.ambulanceRepo = ambulanceRepo;
        this.recommendationRepo = recommendationRepo;
        this.dispatchRecordRepo = dispatchRecordRepo;
    }

    // ---------------------------------------------------------------- registry

    /** Test support: wipe in-memory state between BDD scenarios. */
    public void reset() {
        for (AmbulanceCall call : waitingCalls.snapshot()) {
            waitingCalls.remove(call);
        }
        calls.clear();
        dispatchRecordRepo.deleteAll();
        ambulanceRepo.deleteAll();
    }
    
    
    public void registerAmbulance(Ambulance unit) {
        ambulanceRepo.save(unit);
    }

    public void registerCall(AmbulanceCall call) {
        callRepo.save(call);
    }

    public AmbulanceCall getCall(long callId) {
        return calls.get(callId);        // ← is this what it actually says?
    }

    public Ambulance getAmbulance(long ambulanceId) {
        return ambulanceRepo.findById(ambulanceId).orElse(null);
    }

    public List<AmbulanceCall> waitingCallsSnapshot() {
        return waitingCalls.snapshot();       // adjust to your WaitingCalls method name
    }

    public List<DispatchRecord> getActiveDispatches() {
        return dispatchRecordRepo.findAll();
    }
    // ---------------------------------------------------------------- unchanged

    public void completeEvaluation(AmbulanceCall call, Priority priority,
                                   String requiredCapability, String jurisdiction,
                                   boolean requiresDispatch) {
        call.completeEvaluation(priority, requiredCapability, jurisdiction, requiresDispatch);
        if (requiresDispatch) {
            call.markReadyForDispatch();
            waitingCalls.add(call);
        } else {
            call.cancelDispatchRequirement();
            waitingCalls.remove(call);
        }
        callRepo.save(call);
    }

    public Optional<AmbulanceCall> getNextWaitingCall(long dispatcherId) {
        return waitingCalls.peekUnclaimed(dispatcherId);
    }

    public List<Ambulance> getEligibleAmbulances(AmbulanceCall call) {
        return ambulanceRepo.findAvailableInJurisdiction(call.getJurisdiction()).stream()
                .filter(Ambulance::isDispatchable)
                .filter(a -> a.isAppropriateFor(call))
                .toList();
    }

    public void renewLease(AmbulanceCall call) {
        waitingCalls.renewLease(call);
    }

    public void removeFromQueue(AmbulanceCall call) {
        waitingCalls.remove(call);
    }

    public DispatchRecord confirmDispatch(DispatchRecommendation rec) {
        AmbulanceCall call = rec.getCall();
        Ambulance selected = rec.getSelectedAmbulance();
        Instant dispatchedAt = Instant.now();

        call.assignTo(selected, dispatchedAt);
        selected.markDispatched();

        DispatchRecord record = new DispatchRecord(
                dispatchRecordRepo.nextId(), call, selected, dispatchedAt);

        waitingCalls.remove(call);
        recommendationRepo.delete(rec);
        dispatchRecordRepo.save(record);
        ambulanceRepo.save(selected);
        callRepo.save(call);
        return record;
    }

    public boolean recordAcknowledgement(long dispatchId, Instant at) {
        Optional<DispatchRecord> found = dispatchRecordRepo.findById(dispatchId);
        if (found.isEmpty()) {
            return false;
        }
        DispatchRecord record = found.get();
        record.markAcknowledged(at);
        dispatchRecordRepo.save(record);
        return true;
    }

    public boolean beginResponse(long dispatchId) {
        Optional<DispatchRecord> found = dispatchRecordRepo.findById(dispatchId);
        if (found.isEmpty()) {
            return false;
        }
        DispatchRecord record = found.get();
        record.markInProgress();
        dispatchRecordRepo.save(record);

        Ambulance unit = record.getAmbulance();
        if (unit != null) {
            unit.markEnRoute();
            ambulanceRepo.save(unit);
        }
        return true;
    }

	public void acknowledgeDispatch(long dispatchId, Instant now) {
		// TODO Auto-generated method stub
		
	}

	public void recordCall(AmbulanceCall call) {
	    calls.put(call.getId(), call);
	}
}