package edu.ics240.dispatch.jpa.adapter;

import edu.ics240.dispatch.core.Ambulance;
import edu.ics240.dispatch.core.AmbulanceStatus;
import edu.ics240.dispatch.jpa.entity.JpaAmbulanceEntity;
import org.springframework.stereotype.Component;

/**
 * Simple mapper between JPA entities and domain objects.
 * Keep mapping logic here so domain model stays persistence-agnostic.
 */
@Component
public class JpaMappers {

    public Ambulance toDomain(JpaAmbulanceEntity e) {
        return new Ambulance(
                e.getId(),
                AmbulanceStatus.valueOf(e.getStatus()),
                new edu.ics240.dispatch.core.Location(e.getLatitude(), e.getLongitude()),
                java.util.Set.of(), // capabilities mapping omitted for brevity
                e.getJurisdiction()
        );
    }

    public JpaAmbulanceEntity toEntity(Ambulance a) {
        JpaAmbulanceEntity e = new JpaAmbulanceEntity();
        e.setId(a.getId());
        e.setStatus(a.getStatus().name());
        e.setLatitude(a.getLocation().latitude());
        e.setLongitude(a.getLocation().longitude());
        e.setJurisdiction(a.getJurisdiction());
        return e;
    }
}
