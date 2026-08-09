package edu.ics240.dispatch.inmemory;
 
import edu.ics240.dispatch.core.Ambulance;
import edu.ics240.dispatch.core.AmbulanceCall;
import edu.ics240.dispatch.core.AmbulanceStatus;
import edu.ics240.dispatch.core.Location;
import edu.ics240.dispatch.service.AmbulanceCallCenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
 
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
 
/**
 * Development seed data for the in-memory profile.
 *
 * <p>Mirrors the Background of ED-01-dispatch-ambulance.feature so a manual
 * walkthrough starts from the same state the BDD suite uses: two ALS units in
 * NORTH and one recorded-but-not-yet-evaluated call downtown.
 *
 * <p>Not active under any other profile.
 */
@Profile("inmemory")
@Component
public class DevDataSeeder implements CommandLineRunner {
 
    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
 
    private final AmbulanceCallCenter callCenter;
    private final Clock clock;
 
    public DevDataSeeder(AmbulanceCallCenter callCenter, Clock clock) {
        this.callCenter = callCenter;
        this.clock = clock;
    }
 
    @Override
    public void run(String... args) {
        seedAmbulance(1L, "MEDIC-1", 44.9800, -93.2700, Duration.ofMinutes(30));
        seedAmbulance(2L, "MEDIC-2", 45.2000, -93.6000, Duration.ofMinutes(90));
 
        // 410 Nicollet Mall, downtown Minneapolis. Recorded but not evaluated,
        // so it is not in the waiting queue yet - POST the evaluation to queue it.
        AmbulanceCall call = new AmbulanceCall(1001L, new Location(44.9750, -93.2700), "NORTH");
        callCenter.recordCall(call);
 
        log.info("[seed] MEDIC-1 and MEDIC-2 registered; call 1001 recorded, awaiting evaluation");
    }
 
    private void seedAmbulance(long id, String callSign,
                               double latitude, double longitude, Duration idleFor) {
        Ambulance unit = new Ambulance(
                id,
                AmbulanceStatus.AVAILABLE,
                new Location(latitude, longitude),
                Set.of("ALS", "BLS"),   // an ALS unit can also answer a BLS call
                "NORTH");
        unit.setCallSign(callSign);
        unit.setAvailableSince(clock.instant().minus(idleFor));
        callCenter.registerAmbulance(unit);
    }
}
 