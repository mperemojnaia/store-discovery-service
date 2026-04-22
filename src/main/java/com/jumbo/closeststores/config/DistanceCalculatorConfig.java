package com.jumbo.closeststores.config;

import com.jumbo.closeststores.model.DistanceStrategy;
import com.jumbo.closeststores.service.distance.haversine.HaversineDistanceCalculator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({GoogleMapsProperties.class, OrsProperties.class})
public class DistanceCalculatorConfig {

    private static final Logger log = LoggerFactory.getLogger(DistanceCalculatorConfig.class);

    private final GoogleMapsProperties googleMapsProperties;
    private final OrsProperties orsProperties;

    @Value("${distance.strategy:haversine}")
    private String distanceStrategy;

    public DistanceCalculatorConfig(GoogleMapsProperties googleMapsProperties, OrsProperties orsProperties) {
        this.googleMapsProperties = googleMapsProperties;
        this.orsProperties = orsProperties;
    }

    @PostConstruct
    public void validateApiKeys() {
        log.info("Distance strategy configured: {}", distanceStrategy);

        if (DistanceStrategy.GOOGLE.getValue().equalsIgnoreCase(distanceStrategy)
                && (googleMapsProperties.apiKey() == null || googleMapsProperties.apiKey().isBlank())) {
            throw new IllegalStateException(
                    "Google Distance Matrix API key is required when distance.strategy=google. "
                            + "Set the GOOGLE_MAPS_API_KEY environment variable or google.maps.api-key property.");
        }
        if (DistanceStrategy.ORS.getValue().equalsIgnoreCase(distanceStrategy)
                && (orsProperties.apiKey() == null || orsProperties.apiKey().isBlank())) {
            throw new IllegalStateException(
                    "OpenRouteService API key is required when distance.strategy=ors. "
                            + "Set the ORS_API_KEY environment variable or ors.api-key property.");
        }
    }

    @Bean
    public HaversineDistanceCalculator haversineDistanceCalculator() {
        return new HaversineDistanceCalculator();
    }
}
