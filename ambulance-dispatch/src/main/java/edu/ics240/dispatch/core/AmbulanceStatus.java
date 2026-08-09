package edu.ics240.dispatch.core;

/**
 * High-level lifecycle of an ambulance.
 * This is used by isDispatchable() and state transitions in Steps 2, 7, 9.
 */
public enum AmbulanceStatus {
    AVAILABLE,      // can be dispatched
    DISPATCHED,     // assigned to a call, en route or on scene
    EN_ROUTE,       // responding to the scene
    ON_SCENE,       // at the emergency location
    TRANSPORTING,   // moving patient to hospital
    RETURNING,      // heading back to service area
    OUT_OF_SERVICE  // maintenance, unstaffed, etc.
}