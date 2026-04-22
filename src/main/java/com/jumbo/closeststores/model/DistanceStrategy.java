package com.jumbo.closeststores.model;

import lombok.Getter;

@Getter
public enum DistanceStrategy {
    HAVERSINE("haversine"),
    GOOGLE("google"),
    ORS("ors");

    private final String value;

    DistanceStrategy(String value) {
        this.value = value;
    }

}
