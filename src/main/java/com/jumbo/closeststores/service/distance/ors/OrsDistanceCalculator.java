package com.jumbo.closeststores.service.distance.ors;

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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Primary
@ConditionalOnProperty(name = "distance.strategy", havingValue = "ors")
public class OrsDistanceCalculator implements DistanceCalculator {

    private static final Logger log = LoggerFactory.getLogger(OrsDistanceCalculator.class);

    private static final String PROFILE_DRIVING_CAR = "driving-car";
    private static final String PROFILE_FOOT_WALKING = "foot-walking";
    private static final String PROFILE_CYCLING_REGULAR = "cycling-regular";

    private static final Map<TravelMode, String> TRAVEL_MODE_TO_ORS_PROFILE = Map.of(
            TravelMode.DRIVING, PROFILE_DRIVING_CAR,
            TravelMode.WALKING, PROFILE_FOOT_WALKING,
            TravelMode.BICYCLING, PROFILE_CYCLING_REGULAR,
            TravelMode.TRANSIT, PROFILE_DRIVING_CAR
    );
    private static final String DEFAULT_PROFILE = PROFILE_DRIVING_CAR;

    // Pre-filter: use Haversine to find top N candidates, then refine with ORS
    private static final int CANDIDATE_POOL_SIZE = 15;

    private final OrsDistanceApiClient apiClient;
    private final HaversineDistanceCalculator fallback;

    public OrsDistanceCalculator(
            OrsDistanceApiClient apiClient,
            HaversineDistanceCalculator fallback) {
        this.apiClient = apiClient;
        this.fallback = fallback;
    }

    @Override
    public DistanceResult calculateDistances(Position origin, List<Position> destinations, TravelMode travelMode) {
        String profile = resolveProfile(travelMode);
        long start = System.currentTimeMillis();

        // Step 1: Use Haversine to get approximate distances for ALL stores
        DistanceResult haversineResult = fallback.calculateDistances(origin, destinations, travelMode);
        List<Double> haversineDistances = haversineResult.distances();

        // Step 2: Find indices of the closest N candidates by Haversine
        List<Integer> candidateIndices = findTopCandidateIndices(haversineDistances, CANDIDATE_POOL_SIZE);
        log.debug("ORS refining {} candidates with profile '{}' from ({},{})",
                candidateIndices.size(), profile, origin.latitude(), origin.longitude());

        // Step 3: Refine only the candidates with ORS travel distances
        List<Double> refinedDistances = new ArrayList<>(haversineDistances);
        boolean usedOrs = false;
        int failedCount = 0;

        for (int idx : candidateIndices) {
            Position dest = destinations.get(idx);
            try {
                double orsDistance = apiClient.fetchDistance(origin, dest, profile);
                refinedDistances.set(idx, orsDistance);
                usedOrs = true;
            } catch (HttpClientErrorException.Forbidden e) {
                if (!DEFAULT_PROFILE.equals(profile)) {
                    log.warn("ORS profile '{}' returned 403, retrying with '{}'", profile, DEFAULT_PROFILE);
                    try {
                        double orsDistance = apiClient.fetchDistance(origin, dest, DEFAULT_PROFILE);
                        refinedDistances.set(idx, orsDistance);
                        usedOrs = true;
                        continue;
                    } catch (RestClientException | CallNotPermittedException retryEx) {
                        log.warn("ORS fallback profile also failed for ({},{}): {}",
                                dest.latitude(), dest.longitude(), retryEx.getMessage());
                    }
                }
                failedCount++;
                logFailedCall(dest, e);
            } catch (RestClientException | CallNotPermittedException e) {
                failedCount++;
                logFailedCall(dest, e);
            }
        }

        if (failedCount > 0) {
            log.warn("ORS: {}/{} candidate calls failed, kept Haversine distances for those",
                    failedCount, candidateIndices.size());
        }

        String strategy = usedOrs
                ? DistanceStrategy.ORS.getValue()
                : DistanceStrategy.HAVERSINE.getValue();

        log.debug("ORS calculation completed in {} ms, strategy={}", System.currentTimeMillis() - start, strategy);

        return new DistanceResult(refinedDistances, strategy);
    }

    @Override
    public String getStrategyName() {
        return DistanceStrategy.ORS.getValue();
    }

    private String resolveProfile(TravelMode travelMode) {
        if (travelMode == null) {
            return DEFAULT_PROFILE;
        }
        return TRAVEL_MODE_TO_ORS_PROFILE.getOrDefault(travelMode, DEFAULT_PROFILE);
    }

    private void logFailedCall(Position dest, Exception e) {
        log.debug("ORS call failed for ({},{}): {}", dest.latitude(), dest.longitude(), e.getMessage());
    }

    private List<Integer> findTopCandidateIndices(List<Double> distances, int topN) {
        int n = Math.min(topN, distances.size());
        // Create index list and sort by distance
        List<Integer> indices = new ArrayList<>(distances.size());
        for (int i = 0; i < distances.size(); i++) {
            indices.add(i);
        }
        indices.sort((a, b) -> Double.compare(distances.get(a), distances.get(b)));
        return indices.subList(0, n);
    }
}
