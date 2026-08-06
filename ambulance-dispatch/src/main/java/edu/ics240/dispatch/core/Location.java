package edu.ics240.dispatch.core;

import java.util.Objects;

/**
 * Immutable two-dimensional location used by the ambulance
 * dispatch simulator.
 *
 * A real ambulance system would use GPS coordinates and road-network
 * travel time. This project uses Euclidean distance so the dispatch
 * algorithm remains deterministic and focused on data structures.
 */
public final class Location {

    private final double x;
    private final double y;

    public Location(double x, double y) {
        validateCoordinate(x, "x");
        validateCoordinate(y, "y");

        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /**
     * Calculates squared Euclidean distance.
     *
     * Squared distance is useful when comparing ambulance locations
     * because it avoids performing a square-root calculation for
     * every candidate.
     */
    public double squaredDistanceTo(Location other) {
        Objects.requireNonNull(
                other,
                "Other location cannot be null"
        );

        double deltaX = x - other.x;
        double deltaY = y - other.y;

        return deltaX * deltaX + deltaY * deltaY;
    }

    /**
     * Calculates ordinary Euclidean distance.
     */
    public double distanceTo(Location other) {
        return Math.sqrt(squaredDistanceTo(other));
    }

    private static void validateCoordinate(
            double coordinate,
            String coordinateName) {

        if (!Double.isFinite(coordinate)) {
            throw new IllegalArgumentException(
                    coordinateName
                            + " coordinate must be a finite number"
            );
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Location location)) {
            return false;
        }

        return Double.compare(x, location.x) == 0
                && Double.compare(y, location.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Location{"
                + "x=" + x
                + ", y=" + y
                + '}';
    }
}