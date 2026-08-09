package edu.ics240.dispatch.core;

import java.util.Objects;

/**
 * Outcome of the dispatcher's triage evaluation, carried into STEP 1.
 *
 * <p>Without this payload, priority is never assigned before the call is inserted into a
 * priority-ordered queue, so the queue has no ordering key.
 */
public record EvaluationResult(
        Priority priority,
        RequiredCapability requiredCapability,
        String jurisdiction,
        boolean mutualAidAllowed,
        boolean requiresDispatch,
        String chiefComplaint,
        String sceneHazards) {

    public EvaluationResult {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(requiredCapability, "requiredCapability");
        chiefComplaint = chiefComplaint == null ? "" : chiefComplaint;
        sceneHazards = sceneHazards == null ? "" : sceneHazards;
    }

    /** Evaluation concluding that an ambulance response is required. */
    public static EvaluationResult requiringDispatch(Priority priority,
                                                     RequiredCapability capability,
                                                     String jurisdiction,
                                                     String chiefComplaint) {
        return new EvaluationResult(priority, capability, jurisdiction, true, true, chiefComplaint, "");
    }

    /** Evaluation concluding that no ambulance is needed. */
    public static EvaluationResult notRequiringDispatch(String jurisdiction, String chiefComplaint) {
        return new EvaluationResult(Priority.LOW, RequiredCapability.BLS, jurisdiction,
                false, false, chiefComplaint, "");
    }
}