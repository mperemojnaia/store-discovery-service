package com.jumbo.closeststores.service.distance.google;

import com.jumbo.closeststores.config.GoogleMapsProperties;
import com.jumbo.closeststores.model.DistanceResult;
import com.jumbo.closeststores.model.DistanceStrategy;
import com.jumbo.closeststores.model.Position;
import com.jumbo.closeststores.model.TravelMode;
import com.jumbo.closeststores.service.distance.haversine.HaversineDistanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleDistanceCalculatorTest {

    @Mock
    private GoogleDistanceApiClient apiClient;

    @Mock
    private HaversineDistanceCalculator fallback;

    private GoogleDistanceCalculator calculator;

    private static final GoogleMapsProperties DEFAULT_PROPERTIES =
            new GoogleMapsProperties("test-key", null, 5000, 25);

    @BeforeEach
    void setUp() {
        calculator = new GoogleDistanceCalculator(apiClient, fallback, DEFAULT_PROPERTIES);
    }

    @Test
    void shouldReturnGoogleDistances_whenApiSucceeds() {
        Position origin = new Position(52.0, 4.0);
        List<Position> destinations = List.of(new Position(52.1, 4.1));

        when(apiClient.fetchDistances(eq(origin), eq(destinations), eq("driving")))
                .thenReturn(List.of(5.5));

        DistanceResult result = calculator.calculateDistances(origin, destinations, TravelMode.DRIVING);

        assertAll(
                () -> assertEquals(List.of(5.5), result.distances()),
                () -> assertEquals(DistanceStrategy.GOOGLE, result.strategyUsed())
        );
        verifyNoInteractions(fallback);
    }

    @Test
    void shouldFallBackToHaversine_whenApiFails() {
        Position origin = new Position(52.0, 4.0);
        List<Position> destinations = List.of(new Position(52.1, 4.1));

        when(apiClient.fetchDistances(any(), anyList(), anyString()))
                .thenThrow(new RestClientException("timeout"));
        when(fallback.calculateDistances(eq(origin), eq(destinations), eq(TravelMode.DRIVING)))
                .thenReturn(new DistanceResult(List.of(7.0), DistanceStrategy.HAVERSINE));

        DistanceResult result = calculator.calculateDistances(origin, destinations, TravelMode.DRIVING);

        assertAll(
                () -> assertEquals(List.of(7.0), result.distances()),
                () -> assertEquals(DistanceStrategy.HAVERSINE, result.strategyUsed())
        );
        verify(fallback).calculateDistances(eq(origin), eq(destinations), eq(TravelMode.DRIVING));
    }

    @ParameterizedTest
    @NullSource
    void shouldDefaultToDriving_whenTravelModeIsNull(TravelMode travelMode) {
        Position origin = new Position(52.0, 4.0);
        List<Position> destinations = List.of(new Position(52.1, 4.1));

        when(apiClient.fetchDistances(eq(origin), eq(destinations), eq("driving")))
                .thenReturn(List.of(5.0));

        DistanceResult result = calculator.calculateDistances(origin, destinations, travelMode);

        assertEquals(DistanceStrategy.GOOGLE, result.strategyUsed());
    }

    @Test
    void shouldReturnGoogleStrategy() {
        assertEquals(DistanceStrategy.GOOGLE, calculator.getStrategy());
    }

    @Test
    void shouldBatchDestinations_intoGroupsOfMaxSize() {
        List<Integer> items = List.of(1, 2, 3, 4, 5, 6, 7);

        List<List<Integer>> batches = GoogleDistanceCalculator.batchDestinations(items, 3);

        assertAll(
                () -> assertEquals(3, batches.size()),
                () -> assertEquals(List.of(1, 2, 3), batches.get(0)),
                () -> assertEquals(List.of(4, 5, 6), batches.get(1)),
                () -> assertEquals(List.of(7), batches.get(2))
        );
    }

    @Test
    void shouldReturnSingleBatch_whenDestinationsAreLessThanMax() {
        List<List<Integer>> batches = GoogleDistanceCalculator.batchDestinations(List.of(1, 2), 25);

        assertEquals(1, batches.size());
    }

    @Test
    void shouldReturnEmptyBatches_whenDestinationsAreEmpty() {
        assertTrue(GoogleDistanceCalculator.batchDestinations(List.of(), 25).isEmpty());
    }

    @Test
    void shouldReportHaversineStrategy_whenAnyBatchFallsBack() {
        Position origin = new Position(52.0, 4.0);
        Position dest1 = new Position(52.1, 4.1);
        Position dest2 = new Position(52.2, 4.2);
        List<Position> destinations = List.of(dest1, dest2);

        GoogleMapsProperties smallBatch = new GoogleMapsProperties("key", null, 5000, 1);
        GoogleDistanceCalculator calc = new GoogleDistanceCalculator(apiClient, fallback, smallBatch);

        when(apiClient.fetchDistances(eq(origin), eq(List.of(dest1)), eq("driving")))
                .thenReturn(List.of(5.0));
        when(apiClient.fetchDistances(eq(origin), eq(List.of(dest2)), eq("driving")))
                .thenThrow(new RestClientException("fail"));
        when(fallback.calculateDistances(eq(origin), eq(List.of(dest2)), eq(TravelMode.DRIVING)))
                .thenReturn(new DistanceResult(List.of(8.0), DistanceStrategy.HAVERSINE));

        DistanceResult result = calc.calculateDistances(origin, destinations, TravelMode.DRIVING);

        assertAll(
                () -> assertEquals(2, result.distances().size()),
                () -> assertEquals(5.0, result.distances().getFirst()),
                () -> assertEquals(8.0, result.distances().get(1)),
                () -> assertEquals(DistanceStrategy.HAVERSINE, result.strategyUsed())
        );
    }

    @Test
    void shouldCallApiClient_withCorrectTravelMode() {
        Position origin = new Position(52.0, 4.0);
        List<Position> destinations = List.of(new Position(52.1, 4.1));

        when(apiClient.fetchDistances(eq(origin), eq(destinations), eq("walking")))
                .thenReturn(List.of(6.0));

        calculator.calculateDistances(origin, destinations, TravelMode.WALKING);

        verify(apiClient).fetchDistances(eq(origin), eq(destinations), eq("walking"));
    }
}
