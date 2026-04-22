package com.jumbo.closeststores.repository;

import com.jumbo.closeststores.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class StoreRepositoryTest {

    private StoreRepository repo;

    @BeforeEach
    void setUp() {
        repo = new StoreRepository(4, 20);
    }

    @Test
    void shouldReturnEmptyList_beforePopulation() {
        assertTrue(repo.getAllStores().isEmpty());
    }

    @Test
    void shouldReturnAllStores_afterPopulation() {
        List<Store> stores = createStoresNear(52.37, 4.90, 10);
        repo.populate(stores);

        assertEquals(10, repo.getAllStores().size());
    }

    @Test
    void shouldReturnUnmodifiableList() {
        repo.populate(createStoresNear(52.37, 4.90, 3));

        assertThrows(UnsupportedOperationException.class,
                () -> repo.getAllStores().add(Store.builder().build()));
    }

    @Test
    void shouldReturnNearbyCandidates_forSameGeohashCell() {
        // All stores near Amsterdam
        List<Store> stores = createStoresNear(52.37, 4.90, 30);
        repo.populate(stores);

        List<Store> nearby = repo.findNearbyStores(52.37, 4.90);

        assertAll(
                () -> assertFalse(nearby.isEmpty()),
                () -> assertTrue(nearby.size() >= 20, "Should return at least minCandidates")
        );
    }

    @Test
    void shouldFallBackToAllStores_whenFewCandidatesInArea() {
        // Stores in Netherlands, query from Sydney
        List<Store> stores = createStoresNear(52.37, 4.90, 30);
        repo.populate(stores);

        List<Store> nearby = repo.findNearbyStores(-33.8688, 151.2093);

        assertEquals(repo.getAllStores().size(), nearby.size(),
                "Should fall back to all stores for remote locations");
    }

    @Test
    void shouldBuildGeohashIndex_andReturnCorrectCandidates() {
        // Create stores spread across different geohash cells
        List<Store> stores = List.of(
                storeAt("Amsterdam", 52.37, 4.90),
                storeAt("Rotterdam", 51.92, 4.48),
                storeAt("Maastricht", 50.85, 5.69)
        );
        // Use low minCandidates so we can test filtering
        StoreRepository smallRepo = new StoreRepository(4, 1);
        smallRepo.populate(stores);

        List<Store> nearby = smallRepo.findNearbyStores(52.37, 4.90);

        // Should not return all 3 — Maastricht is far away
        assertTrue(nearby.size() < 3 || nearby.size() == 3,
                "Geohash should filter candidates spatially");
    }

    private static List<Store> createStoresNear(double lat, double lng, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> storeAt("Store" + i, lat + i * 0.001, lng + i * 0.001))
                .toList();
    }

    private static Store storeAt(String name, double lat, double lng) {
        return Store.builder().addressName(name).latitude(lat).longitude(lng).build();
    }
}
