package com.jumbo.closeststores.service.distance.haversine;

import com.jumbo.closeststores.model.DistanceResult;
import com.jumbo.closeststores.model.DistanceStrategy;
import com.jumbo.closeststores.model.Position;
import com.jumbo.closeststores.model.TravelMode;
import com.jumbo.closeststores.service.DistanceCalculator;

import java.util.ArrayList;
import java.util.List;

public class HaversineDistanceCalculator implements DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public double calculate(Position a, Position b) {
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = Math.toRadians(b.latitude() - a.latitude());
        double dLng = Math.toRadians(b.longitude() - a.longitude());

        double haversine = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return EARTH_RADIUS_KM * c;
    }

    @Override
    public DistanceResult calculateDistances(Position origin, List<Position> destinations, TravelMode travelMode) {
        List<Double> distances = new ArrayList<>(destinations.size());
        for (Position destination : destinations) {
            distances.add(calculate(origin, destination));
        }
        return new DistanceResult(distances, getStrategyName());
    }

    @Override
    public String getStrategyName() {
        return DistanceStrategy.HAVERSINE.getValue();
    }
}
