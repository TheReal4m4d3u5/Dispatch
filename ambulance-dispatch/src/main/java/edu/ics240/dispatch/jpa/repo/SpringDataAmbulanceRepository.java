package edu.ics240.dispatch.jpa.repo;

import edu.ics240.dispatch.jpa.entity.JpaAmbulanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataAmbulanceRepository extends JpaRepository<JpaAmbulanceEntity, Long> {
    List<JpaAmbulanceEntity> findByJurisdictionAndStatus(String jurisdiction, String status);
}
