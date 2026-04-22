package com.jumbo.closeststores.service;

import com.jumbo.closeststores.model.DistanceResult;
import com.jumbo.closeststores.model.Position;
import com.jumbo.closeststores.model.TravelMode;

import java.util.List;

public interface DistanceCalculator {
    DistanceResult calculateDistances(Position origin, List<Position> destinations, TravelMode travelMode);
    String getStrategyName();
}
