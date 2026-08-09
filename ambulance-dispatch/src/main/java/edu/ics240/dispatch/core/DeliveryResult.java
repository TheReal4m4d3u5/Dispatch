package edu.ics240.dispatch.core;

public enum DeliveryResult {
    DELIVERED,
    FAILED,
    TIMEOUT;

    /** No crew is assigned to the unit, so there is nobody to notify. */
    public static DeliveryResult noCrew() {
        return FAILED;
    }

    /** The assignment reached the given device. */
    public static DeliveryResult delivered(Object deviceId) {
        return DELIVERED;
    }

    /** The device could not be reached. */
    public static DeliveryResult unreachable(Object deviceId) {
        return FAILED;
    }

    public boolean isDelivered() {
        return this == DELIVERED;
    }
}