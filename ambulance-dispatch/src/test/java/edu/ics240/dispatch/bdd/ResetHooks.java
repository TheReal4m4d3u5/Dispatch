package edu.ics240.dispatch.bdd;

import edu.ics240.dispatch.service.AmbulanceCallCenter;
import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;

public class ResetHooks {

    @Autowired private AmbulanceCallCenter callCenter;

    @Before
    public void resetBetweenScenarios() {
        callCenter.reset();
    }
}