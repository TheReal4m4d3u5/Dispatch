package edu.ics240.dispatch.core;

/**
 * Clinical capability tiers. Higher ordinal subsumes lower: an ALS unit can take a BLS call.
 */
public enum RequiredCapability {
    BLS, ALS, CRITICAL_CARE;

    public boolean satisfies(RequiredCapability required) {
        return this.ordinal() >= required.ordinal();
    }
}
