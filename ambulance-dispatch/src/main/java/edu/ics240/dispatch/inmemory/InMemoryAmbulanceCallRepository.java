package edu.ics240.dispatch.inmemory;

import edu.ics240.dispatch.core.AmbulanceCall;
import edu.ics240.dispatch.core.AmbulanceCallRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!jpa")
public class InMemoryAmbulanceCallRepository implements AmbulanceCallRepository {

    private final Map<Long, AmbulanceCall> calls = new ConcurrentHashMap<>();

    @Override
    public AmbulanceCall save(AmbulanceCall call) {
        calls.put(call.getId(), call);
        return call;
    }

    @Override
    public Optional<AmbulanceCall> findById(long callId) {
        return Optional.ofNullable(calls.get(callId));
    }

    public List<AmbulanceCall> findAll() {
        return calls.values().stream()
        		.sorted(Comparator.comparingLong(AmbulanceCall::getId))
                .toList();
    }

    @Override
    public void delete(long id) {
        calls.remove(id);
    }
}
