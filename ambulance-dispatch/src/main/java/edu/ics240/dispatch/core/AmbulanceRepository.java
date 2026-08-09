package edu.ics240.dispatch.core;

import java.util.List;
import java.util.Optional;

/**
 * Domain-level repository interface for Ambulance aggregate.
 * Keep the interface small and focused on domain needs.
 */
public interface AmbulanceRepository {
    Optional<Ambulance> findById(long id);
    List<Ambulance> findAvailableInJurisdiction(String jurisdiction);
    Ambulance save(Ambulance ambulance);
    void delete(long id);
    void deleteAll();
}
