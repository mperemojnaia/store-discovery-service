package com.jumbo.closeststores.model;

import java.util.List;

public record DistanceResult(
    List<Double> distances,
    String strategyUsed  // "haversine" or "google", or "ors"
) {}
