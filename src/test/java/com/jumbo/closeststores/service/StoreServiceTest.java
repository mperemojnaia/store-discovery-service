package com.jumbo.closeststores.service;

import com.jumbo.closeststores.model.*;
import com.jumbo.closeststores.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private DistanceCalculator distanceCalculator;

    @InjectMocks
    private StoreService storeService;

    private static Store storeAt(String name, double lat, double lng) {
        return Store.builder().addressName(name).latitude(lat).longitude(lng).build();
    }

    @Test
    void shouldReturnAtMost5Stores_whenMoreThan5AreAvailable() {
        List<Store> stores = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> storeAt("Store" + i, 52.0 + i * 0.01, 4.0))
                .toList();
        List<Double> distances = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> (double) i)
                .toList();

        when(storeRepository.findNearbyStores(eq(52.0), eq(4.0))).thenReturn(stores);
        when(distanceCalculator.calculateDistances(any(), anyList(), (TravelMode) isNull()))
                .thenReturn(new DistanceResult(distances, "haversine"));

        StoreLocatorResult result = storeService.findClosestStores(52.0, 4.0, null);

        assertEquals(5, result.stores().size());
    }

    @Test
    void shouldReturnAllStores_whenFewerThan5AreAvailable() {
        List<Store> stores = List.of(
                storeAt("A", 52.1, 4.1),
                storeAt("B", 52.2, 4.2)
        );

        when(storeRepository.findNearbyStores(eq(52.0), eq(4.0))).thenReturn(stores);
        when(distanceCalculator.calculateDistances(any(), anyList(), (TravelMode) isNull()))
                .thenReturn(new DistanceResult(List.of(5.0, 3.0), "haversine"));

        StoreLocatorResult result = storeService.findClosestStores(52.0, 4.0, null);

        assertEquals(2, result.stores().size());
    }

    @Test
    void shouldSortStoresByDistanceAscending() {
        List<Store> stores = List.of(
                storeAt("Far", 53.0, 5.0),
                storeAt("Close", 52.1, 4.1),
                storeAt("Mid", 52.5, 4.5)
        );

        when(storeRepository.findNearbyStores(eq(52.0), eq(4.0))).thenReturn(stores);
        when(distanceCalculator.calculateDistances(any(), anyList(), (TravelMode) isNull()))
                .thenReturn(new DistanceResult(List.of(100.0, 1.0, 50.0), "haversine"));

        StoreLocatorResult result = storeService.findClosestStores(52.0, 4.0, null);

        assertAll(
                () -> assertEquals("Close", result.stores().get(0).store().addressName()),
                () -> assertEquals("Mid", result.stores().get(1).store().addressName()),
                () -> assertEquals("Far", result.stores().get(2).store().addressName())
        );
    }

    @Test
    void shouldRoundDistancesToTwoDecimalPlaces() {
        List<Store> stores = List.of(storeAt("A", 52.1, 4.1));

        when(storeRepository.findNearbyStores(eq(52.0), eq(4.0))).thenReturn(stores);
        when(distanceCalculator.calculateDistances(any(), anyList(), (TravelMode) isNull()))
                .thenReturn(new DistanceResult(List.of(12.3456789), "haversine"));

        StoreLocatorResult result = storeService.findClosestStores(52.0, 4.0, null);

        assertEquals(12.35, result.stores().get(0).distanceKm());
    }

    @Test
    void shouldIncludeDistanceType_fromCalculator() {
        List<Store> stores = List.of(storeAt("A", 52.1, 4.1));

        when(storeRepository.findNearbyStores(eq(52.0), eq(4.0))).thenReturn(stores);
        when(distanceCalculator.calculateDistances(any(), anyList(), eq(TravelMode.WALKING)))
                .thenReturn(new DistanceResult(List.of(5.0), "google"));

        StoreLocatorResult result = storeService.findClosestStores(52.0, 4.0, TravelMode.WALKING);

        assertEquals("google", result.distanceType());
    }

    @Test
    void shouldThrowException_whenCoordinatesAreInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> storeService.findClosestStores(91.0, 4.0, null));
    }
}
