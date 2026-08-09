package edu.ics240.dispatch.core;

import java.util.List;
import java.util.Optional;

public interface DispatchRecordRepository {
    DispatchRecord save(DispatchRecord record);
    Optional<DispatchRecord> findById(long id);
    long nextId();
	List<DispatchRecord> findAll();
	
    void deleteAll();
	
}
