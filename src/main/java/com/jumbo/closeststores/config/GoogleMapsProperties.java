package com.jumbo.closeststores.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.maps")
public record GoogleMapsProperties(
        String apiKey,
        String baseUrl,
        int timeout,
        int maxDestinationsPerRequest
) {
    public GoogleMapsProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://maps.googleapis.com/maps/api/distancematrix/json";
        }
        if (timeout <= 0) {
            timeout = 5000;
        }
        if (maxDestinationsPerRequest <= 0) {
            maxDestinationsPerRequest = 25;
        }
    }
}
