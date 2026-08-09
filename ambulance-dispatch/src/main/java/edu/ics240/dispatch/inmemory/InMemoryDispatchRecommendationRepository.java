package edu.ics240.dispatch.inmemory;

import edu.ics240.dispatch.core.DispatchRecommendation;
import edu.ics240.dispatch.core.DispatchRecommendationRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Profile("inmemory")
@Repository
public class InMemoryDispatchRecommendationRepository implements DispatchRecommendationRepository {

    private final Map<Long, DispatchRecommendation> store = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public DispatchRecommendation save(DispatchRecommendation rec) {
        store.put(rec.getId(), rec);
        return rec;
    }

    @Override
    public Optional<DispatchRecommendation> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void delete(DispatchRecommendation rec) {
        store.remove(rec.getId());
    }

    @Override
    public long nextId() {
        return idGen.getAndIncrement();
    }
}
