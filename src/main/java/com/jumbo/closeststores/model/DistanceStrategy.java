package com.jumbo.closeststores.model;

public enum DistanceStrategy {
    HAVERSINE("haversine"),
    GOOGLE("google"),
    ORS("ors");

    private final String value;

    DistanceStrategy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
