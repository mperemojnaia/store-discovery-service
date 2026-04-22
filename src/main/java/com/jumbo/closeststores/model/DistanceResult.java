package com.jumbo.closeststores.model;

import java.util.List;

public record DistanceResult(
    List<Double> distances,
    DistanceStrategy strategyUsed
) {}
