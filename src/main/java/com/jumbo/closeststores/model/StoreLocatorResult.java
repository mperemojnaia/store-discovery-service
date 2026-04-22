package com.jumbo.closeststores.model;

import java.util.List;

public record StoreLocatorResult(List<StoreWithDistance> stores, String distanceType) {}
