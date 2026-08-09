package edu.ics240.dispatch.core;

/**
 * Where the selected ambulance came from.
 * Step 5 uses this to distinguish CAD vs dispatcher override.
 */
public enum SelectionSource {
    CAD_RECOMMENDED,
    DISPATCHER_OVERRIDE
}
