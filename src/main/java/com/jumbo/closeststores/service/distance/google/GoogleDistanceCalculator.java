package com.jumbo.closeststores.service.distance.google;

import com.jumbo.closeststores.config.GoogleMapsProperties;
import com.jumbo.closeststores.model.DistanceResult;
import com.jumbo.closeststores.model.DistanceStrategy;
import com.jumbo.closeststores.model.Position;
import com.jumbo.closeststores.model.TravelMode;
import com.jumbo.closeststores.service.DistanceCalculator;
import com.jumbo.closeststores.service.distance.haversine.HaversineDistanceCalculator;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Component
@Primary
@ConditionalOnProperty(name = "distance.strategy", havingValue = "google")
public class GoogleDistanceCalculator implements DistanceCalculator {

    private static final Logger log = LoggerFactory.getLogger(GoogleDistanceCalculator.class);

    private final GoogleDistanceApiClient apiClient;
    private final HaversineDistanceCalculator fallback;
    private final GoogleMapsProperties properties;

    public GoogleDistanceCalculator(
            GoogleDistanceApiClient apiClient,
            HaversineDistanceCalculator fallback,
            GoogleMapsProperties properties) {
        this.apiClient = apiClient;
        this.fallback = fallback;
        this.properties = properties;
    }

    @Override
    public DistanceResult calculateDistances(Position origin, List<Position> destinations, TravelMode travelMode) {
        String mode = travelMode != null ? travelMode.getValue() : TravelMode.DRIVING.getValue();
        long start = System.currentTimeMillis();

        List<List<Position>> batches = batchDestinations(destinations, properties.maxDestinationsPerRequest());
        List<Double> allDistances = new ArrayList<>(destinations.size());
        boolean usedFallback = false;

        for (List<Position> batch : batches) {
            try {
                allDistances.addAll(apiClient.fetchDistances(origin, batch, mode));
            } catch (RestClientException | CallNotPermittedException e) {
                log.warn("Batch failed, falling back to Haversine for {} destinations: {}",
                        batch.size(), e.getMessage());
                allDistances.addAll(fallback.calculateDistances(origin, batch, travelMode).distances());
                usedFallback = true;
            }
        }

        String strategy = usedFallback
                ? DistanceStrategy.HAVERSINE.getValue()
                : DistanceStrategy.GOOGLE.getValue();

        log.debug("Google calculation completed in {} ms, {} batches, strategy={}",
                System.currentTimeMillis() - start, batches.size(), strategy);

        return new DistanceResult(allDistances, strategy);
    }

    @Override
    public String getStrategyName() {
        return DistanceStrategy.GOOGLE.getValue();
    }

    /**
     * Partitions a list into batches of at most maxPerBatch elements.
     * Package-private for testability.
     */
    static <T> List<List<T>> batchDestinations(List<T> destinations, int maxPerBatch) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < destinations.size(); i += maxPerBatch) {
            batches.add(destinations.subList(i, Math.min(i + maxPerBatch, destinations.size())));
        }
        return batches;
    }
}
