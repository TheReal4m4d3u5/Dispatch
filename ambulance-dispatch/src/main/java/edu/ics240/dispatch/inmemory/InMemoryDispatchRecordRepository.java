package edu.ics240.dispatch.inmemory;

import edu.ics240.dispatch.core.DispatchRecord;
import edu.ics240.dispatch.core.DispatchRecordRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Profile("inmemory")
@Repository
public class InMemoryDispatchRecordRepository implements DispatchRecordRepository {

    private final Map<Long, DispatchRecord> store = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public DispatchRecord save(DispatchRecord record) {
        store.put(record.getId(), record);
        return record;
    }

    @Override
    public Optional<DispatchRecord> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public long nextId() {
        return idGen.getAndIncrement();
    }

    @Override
    public List<DispatchRecord> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
