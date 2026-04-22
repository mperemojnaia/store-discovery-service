package com.jumbo.closeststores.model;

import com.jumbo.closeststores.controller.exception.InvalidRequestException;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum TravelMode {
    DRIVING("driving"),
    WALKING("walking"),
    BICYCLING("bicycling"),
    TRANSIT("transit");

    private final String value;

    private static final Map<String, TravelMode> BY_VALUE =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            m -> m.value.toLowerCase(),
                            m -> m
                    ));

    TravelMode(String value) {
        this.value = value;
    }

    public static TravelMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("Travel mode must not be null or empty");
        }

        TravelMode mode = BY_VALUE.get(value.toLowerCase());
        if (mode == null) {
            throw new InvalidRequestException(
                    "Invalid travel mode: '" + value + "'. Allowed: " + BY_VALUE.keySet());
        }
        return mode;
    }
}
