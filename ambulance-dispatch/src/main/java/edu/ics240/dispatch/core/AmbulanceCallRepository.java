package edu.ics240.dispatch.core;

import java.util.Optional;

public interface AmbulanceCallRepository {
    Optional<AmbulanceCall> findById(long id);
    AmbulanceCall save(AmbulanceCall call);
    void delete(long id);
}
