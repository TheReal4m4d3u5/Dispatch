package edu.ics240.dispatch.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

/**
 * Entry point for the Cucumber suite. Runs under {@code mvn test} alongside the JUnit
 * tests, so the narrative scenarios and the unit tests fail the same build.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "edu.ics240.dispatch.bdd")
public class RunCucumberTest {
}
