package com.jumbo.closeststores.repository;

import com.jumbo.closeststores.model.Store;
import ch.hsr.geohash.GeoHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory store repository with geohash-based spatial indexing.
 * Data is populated by {@link StoreDataLoader} on application startup.
 * Thread-safe: uses an immutable snapshot swapped atomically via {@link AtomicReference}.
 */
@Repository
public class StoreRepository {

    private static final Logger log = LoggerFactory.getLogger(StoreRepository.class);

    private final int geohashPrecision;
    private final int minCandidates;

    /** Immutable snapshot of stores + geohash index, swapped atomically. */
    private final AtomicReference<StoreSnapshot> snapshot =
            new AtomicReference<>(new StoreSnapshot(List.of(), Map.of()));

    public StoreRepository(
            @Value("${store.geohash.precision:4}") int geohashPrecision,
            @Value("${store.geohash.min-candidates:20}") int minCandidates) {
        this.geohashPrecision = geohashPrecision;
        this.minCandidates = minCandidates;
    }

    /**
     * Populates the repository with store data and builds the geohash index.
     * Called by {@link StoreDataLoader} after data is loaded.
     */
    public void populate(List<Store> storeData) {
        Map<String, List<Store>> index = new HashMap<>();
        for (Store store : storeData) {
            String hash = GeoHash.withCharacterPrecision(
                    store.latitude(), store.longitude(), geohashPrecision).toBase32();
            index.computeIfAbsent(hash, k -> new ArrayList<>()).add(store);
        }

        snapshot.set(new StoreSnapshot(
                List.copyOf(storeData),
                Collections.unmodifiableMap(index)
        ));

        log.info("Indexed {} stores into {} geohash cells (precision={})",
                storeData.size(), index.size(), geohashPrecision);
    }

    public List<Store> getAllStores() {
        return snapshot.get().stores();
    }

    /**
     * Returns stores in the same and neighboring geohash cells as the given coordinates.
     * Falls back to all stores if the spatial lookup yields too few candidates.
     */
    public List<Store> findNearbyStores(double latitude, double longitude) {
        StoreSnapshot current = snapshot.get();

        GeoHash center = GeoHash.withCharacterPrecision(latitude, longitude, geohashPrecision);
        GeoHash[] adjacent = center.getAdjacent();

        Set<Store> candidates = new LinkedHashSet<>();
        addStoresForCell(candidates, center.toBase32(), current.geohashIndex());
        for (GeoHash neighbor : adjacent) {
            addStoresForCell(candidates, neighbor.toBase32(), current.geohashIndex());
        }

        if (candidates.size() < minCandidates) {
            log.debug("Geohash lookup returned only {} candidates, falling back to all {} stores",
                    candidates.size(), current.stores().size());
            return current.stores();
        }

        log.debug("Geohash pre-filter: {} candidates from {} cells", candidates.size(), adjacent.length + 1);
        return List.copyOf(candidates);
    }

    private void addStoresForCell(Set<Store> candidates, String cellHash,
                                  Map<String, List<Store>> index) {
        List<Store> bucket = index.get(cellHash);
        if (bucket != null) {
            candidates.addAll(bucket);
        }
    }

    /** Immutable snapshot holding stores and their geohash index together. */
    private record StoreSnapshot(List<Store> stores, Map<String, List<Store>> geohashIndex) {}
}
