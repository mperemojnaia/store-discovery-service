package com.jumbo.closeststores.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ors")
public record OrsProperties(
        String apiKey,
        String baseUrl,
        int timeout,
        int maxLocationsPerRequest
) {
    public OrsProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openrouteservice.org/v2/matrix";
        }
        if (timeout <= 0) {
            timeout = 5000;
        }
        if (maxLocationsPerRequest <= 0) {
            maxLocationsPerRequest = 50;
        }
    }
}
