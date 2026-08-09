package edu.ics240.dispatch.core;

/**
 * Value object representing a geographic location.
 * Used by Step 3 for distance and ETA calculations.
 */
public final class Location {

    private final double latitude;
    private final double longitude;

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double latitude() { return latitude; }
    public double longitude() { return longitude; }

    /**
     * Haversine distance in kilometers.
     * Step 3 uses this as a lower bound heuristic for road distance.
     */
    public double distanceTo(Location other) {
        double R = 6371.0; // Earth radius in km
        double latRad1 = Math.toRadians(this.latitude);
        double latRad2 = Math.toRadians(other.latitude);
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(latRad1) * Math.cos(latRad2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
