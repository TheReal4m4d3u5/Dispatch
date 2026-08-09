package edu.ics240.dispatch.jpa.adapter;

import edu.ics240.dispatch.core.Ambulance;
import edu.ics240.dispatch.core.AmbulanceRepository;
import edu.ics240.dispatch.jpa.entity.JpaAmbulanceEntity;
import edu.ics240.dispatch.jpa.repo.SpringDataAmbulanceRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Profile("jpa")
@Repository
public class JpaAmbulanceRepositoryAdapter implements AmbulanceRepository {

    private final SpringDataAmbulanceRepository repo;
    private final JpaMappers mapper;

    public JpaAmbulanceRepositoryAdapter(SpringDataAmbulanceRepository repo, JpaMappers mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    public Optional<Ambulance> findById(long id) {
        return repo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Ambulance> findAvailableInJurisdiction(String jurisdiction) {
        return repo.findByJurisdictionAndStatus(jurisdiction, "AVAILABLE")
                   .stream()
                   .map(mapper::toDomain)
                   .collect(Collectors.toList());
    }

    @Override
    public Ambulance save(Ambulance ambulance) {
        JpaAmbulanceEntity entity = mapper.toEntity(ambulance);
        JpaAmbulanceEntity saved = repo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(long id) {
    	
        repo.deleteById(id);
    }
    
    @Override
    public void deleteAll() {
        repo.deleteAll();
    }
}
