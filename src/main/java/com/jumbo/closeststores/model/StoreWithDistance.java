package com.jumbo.closeststores.model;

public record StoreWithDistance(Store store, double distanceKm)
    implements Comparable<StoreWithDistance> {

    @Override
    public int compareTo(StoreWithDistance other) {
        return Double.compare(this.distanceKm, other.distanceKm);
    }
}
