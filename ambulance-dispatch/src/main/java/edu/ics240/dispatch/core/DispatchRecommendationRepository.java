package edu.ics240.dispatch.core;

import java.util.Optional;

public interface DispatchRecommendationRepository {
    DispatchRecommendation save(DispatchRecommendation rec);
    Optional<DispatchRecommendation> findById(long id);
    void delete(DispatchRecommendation rec);
    long nextId();
}
