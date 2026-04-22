package com.jumbo.closeststores.model;

public record Position(double latitude, double longitude) {

    public static final double MIN_LATITUDE = -90;
    public static final double MAX_LATITUDE = 90;
    public static final double MIN_LONGITUDE = -180;
    public static final double MAX_LONGITUDE = 180;

    public Position {
        if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
            throw new IllegalArgumentException(
                    "Latitude must be between " + MIN_LATITUDE + " and " + MAX_LATITUDE);
        }
        if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
            throw new IllegalArgumentException(
                    "Longitude must be between " + MIN_LONGITUDE + " and " + MAX_LONGITUDE);
        }
    }
}
