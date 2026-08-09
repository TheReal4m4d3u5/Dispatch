package edu.ics240.dispatch.inmemory;

import edu.ics240.dispatch.core.Ambulance;
import edu.ics240.dispatch.core.AmbulanceRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple thread-safe in-memory repository for development and tests.
 * Annotated with @Profile("inmemory") so Spring will only load it when that profile is active.
 */
@Profile("inmemory")
@Repository
public class InMemoryAmbulanceRepository implements AmbulanceRepository {

    private final Map<Long, Ambulance> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Ambulance> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Ambulance> findAvailableInJurisdiction(String jurisdiction) {
        return store.values().stream()
                .filter(a -> jurisdiction.equals(a.getJurisdiction()))
                .filter(Ambulance::isDispatchable)
                .toList();
    }

    @Override
    public Ambulance save(Ambulance ambulance) {
        store.put(ambulance.getId(), ambulance);
        return ambulance;
    }
    
    @Override
    public void deleteAll() {
        store.clear();
    }

    @Override
    public void delete(long id) {
        store.remove(id);
    }
}
