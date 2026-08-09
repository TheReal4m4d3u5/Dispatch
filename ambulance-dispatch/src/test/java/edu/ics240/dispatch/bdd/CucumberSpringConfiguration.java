package edu.ics240.dispatch.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Hands assembly to Spring.
 *
 * <p>
 * Without this, the test fixture has to call every constructor by hand, which
 * means the suite breaks whenever a constructor changes. With it, Spring
 * resolves the wiring exactly as it does at runtime and the tests never name a
 * constructor at all.
 *
 * <p>
 * {@code @DirtiesContext} rebuilds the context before each scenario. The
 * in-memory repositories are singletons, so without it a call registered in one
 * scenario would still be in the queue for the next. It costs a few hundred
 * milliseconds per scenario, which is a fair price for isolation at this suite
 * size.
 */
@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("inmemory")
// @DirtiesContext   <-- delete
public class CucumberSpringConfiguration {
}