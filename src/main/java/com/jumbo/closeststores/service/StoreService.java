package com.jumbo.closeststores.service;

import com.jumbo.closeststores.model.StoreLocatorResult;
import com.jumbo.closeststores.model.DistanceResult;
import com.jumbo.closeststores.model.Position;
import com.jumbo.closeststores.model.Store;
import com.jumbo.closeststores.model.StoreWithDistance;
import com.jumbo.closeststores.model.TravelMode;
import com.jumbo.closeststores.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private static final Logger log = LoggerFactory.getLogger(StoreService.class);

    private static final int MAX_RESULTS = 5;
    private static final double DISTANCE_ROUNDING_FACTOR = 100.0; // 2 decimal places

    private final StoreRepository storeRepository;
    private final DistanceCalculator distanceCalculator;

    public StoreLocatorResult findClosestStores(double lat, double lng, TravelMode travelMode) {
        Position origin = new Position(lat, lng);

        List<Store> nearbyStores = storeRepository.findNearbyStores(lat, lng);
        log.debug("Calculating distances for {} candidate stores from ({}, {})", nearbyStores.size(), lat, lng);

        List<Position> storePositions = nearbyStores.stream()
                .map(store -> new Position(store.latitude(), store.longitude()))
                .toList();

        DistanceResult distanceResult = distanceCalculator.calculateDistances(origin, storePositions, travelMode);
        List<Double> distances = distanceResult.distances();

        List<StoreWithDistance> storesWithDistance = new ArrayList<>(nearbyStores.size());
        for (int i = 0; i < nearbyStores.size(); i++) {
            double roundedDistance = Math.round(distances.get(i) * DISTANCE_ROUNDING_FACTOR) / DISTANCE_ROUNDING_FACTOR;
            storesWithDistance.add(new StoreWithDistance(nearbyStores.get(i), roundedDistance));
        }

        Collections.sort(storesWithDistance);

        List<StoreWithDistance> closest = storesWithDistance.size() <= MAX_RESULTS
                ? storesWithDistance
                : storesWithDistance.subList(0, MAX_RESULTS);

        log.debug("Closest store: '{}' at {} km", closest.get(0).store().addressName(),
                closest.get(0).distanceKm());

        return new StoreLocatorResult(closest, distanceResult.strategyUsed());
    }
}
