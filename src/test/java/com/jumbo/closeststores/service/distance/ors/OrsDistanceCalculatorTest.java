package com.jumbo.closeststores.service.distance.ors;

import com.jumbo.closeststores.model.DistanceResult;
import com.jumbo.closeststores.model.DistanceStrategy;
import com.jumbo.closeststores.model.Position;
import com.jumbo.closeststores.model.TravelMode;
import com.jumbo.closeststores.service.distance.haversine.HaversineDistanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrsDistanceCalculatorTest {

    @Mock
    private OrsDistanceApiClient apiClient;

    private HaversineDistanceCalculator fallback;
    private OrsDistanceCalculator calculator;

    private static final Position ORIGIN = new Position(52.3676, 4.9041);

    @BeforeEach
    void setUp() {
        fallback = new HaversineDistanceCalculator();
        calculator = new OrsDistanceCalculator(apiClient, fallback, 15);
    }

    @Nested
    class HappyPath {

        @Test
        void shouldRefineTopCandidatesWithOrsDistances() {
            List<Position> destinations = createDestinations(20);

            when(apiClient.fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car")))
                    .thenReturn(2.5);

            DistanceResult result = calculator.calculateDistances(ORIGIN, destinations, TravelMode.DRIVING);

            assertAll(
                    () -> assertEquals(DistanceStrategy.ORS, result.strategyUsed()),
                    () -> assertEquals(20, result.distances().size()),
                    () -> assertTrue(result.distances().stream().anyMatch(d -> d == 2.5),
                            "Should contain ORS-refined distances")
            );
            // Should only call ORS for top 15 candidates, not all 20
            verify(apiClient, times(15)).fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car"));
        }

        @Test
        void shouldReturnOrsStrategy_whenAtLeastOneCandidateSucceeds() {
            List<Position> destinations = createDestinations(3);

            when(apiClient.fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car")))
                    .thenReturn(1.0)
                    .thenThrow(new RestClientException("timeout"))
                    .thenReturn(3.0);

            DistanceResult result = calculator.calculateDistances(ORIGIN, destinations, TravelMode.DRIVING);

            assertEquals(DistanceStrategy.ORS, result.strategyUsed());
        }
    }

    @Nested
    class ProfileMapping {

        @Test
        void shouldUseFootWalkingProfile_forWalkingMode() {
            List<Position> destinations = createDestinations(1);

            when(apiClient.fetchDistance(eq(ORIGIN), any(Position.class), eq("foot-walking")))
                    .thenReturn(1.5);

            calculator.calculateDistances(ORIGIN, destinations, TravelMode.WALKING);

            verify(apiClient).fetchDistance(eq(ORIGIN), any(Position.class), eq("foot-walking"));
        }

        @Test
        void shouldUseCyclingRegularProfile_forBicyclingMode() {
            List<Position> destinations = createDestinations(1);

            when(apiClient.fetchDistance(eq(ORIGIN), any(Position.class), eq("cycling-regular")))
                    .thenReturn(1.5);

            calculator.calculateDistances(ORIGIN, destinations, TravelMode.BICYCLING);

            verify(apiClient).fetchDistance(eq(ORIGIN), any(Position.class), eq("cycling-regular"));
        }

        @Test
        void shouldUseDrivingCarProfile_forTransitMode() {
            List<Position> destinations = createDestinations(1);

            when(apiClient.fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car")))
                    .thenReturn(1.5);

            calculator.calculateDistances(ORIGIN, destinations, TravelMode.TRANSIT);

            verify(apiClient).fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car"));
        }

        @ParameterizedTest
        @NullSource
        void shouldDefaultToDrivingCar_whenTravelModeIsNull(TravelMode travelMode) {
            List<Position> destinations = createDestinations(1);

            when(apiClient.fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car")))
                    .thenReturn(1.5);

            calculator.calculateDistances(ORIGIN, destinations, travelMode);

            verify(apiClient).fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car"));
        }
    }

    @Nested
    class FallbackBehavior {

        @Test
        void shouldFallBackToHaversine_whenAllOrsCandidatesFail() {
            List<Position> destinations = createDestinations(3);

            when(apiClient.fetchDistance(eq(ORIGIN), any(Position.class), anyString()))
                    .thenThrow(new RestClientException("timeout"));

            DistanceResult result = calculator.calculateDistances(ORIGIN, destinations, TravelMode.DRIVING);

            assertAll(
                    () -> assertEquals(DistanceStrategy.HAVERSINE, result.strategyUsed()),
                    () -> assertEquals(3, result.distances().size()),
                    () -> assertTrue(result.distances().stream().allMatch(d -> d > 0),
                            "Should have Haversine distances as fallback")
            );
        }

        @Test
        void shouldRetryWithDefaultProfile_when403OnCustomProfile() {
            List<Position> destinations = createDestinations(1);

            when(apiClient.fetchDistance(eq(ORIGIN), any(Position.class), eq("foot-walking")))
                    .thenThrow(HttpClientErrorException.create(
                            HttpStatus.FORBIDDEN, "Forbidden", null, null, null));
            when(apiClient.fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car")))
                    .thenReturn(3.0);

            DistanceResult result = calculator.calculateDistances(ORIGIN, destinations, TravelMode.WALKING);

            assertAll(
                    () -> assertEquals(DistanceStrategy.ORS, result.strategyUsed()),
                    () -> verify(apiClient).fetchDistance(eq(ORIGIN), any(Position.class), eq("foot-walking")),
                    () -> verify(apiClient).fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car"))
            );
        }

        @Test
        void shouldNotRetryWithDefaultProfile_whenAlreadyUsingDefaultProfile() {
            List<Position> destinations = createDestinations(1);

            when(apiClient.fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car")))
                    .thenThrow(HttpClientErrorException.create(
                            HttpStatus.FORBIDDEN, "Forbidden", null, null, null));

            DistanceResult result = calculator.calculateDistances(ORIGIN, destinations, TravelMode.DRIVING);

            assertEquals(DistanceStrategy.HAVERSINE, result.strategyUsed());
            // Should only call once — no retry since already using default profile
            verify(apiClient, times(1)).fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car"));
        }

        @Test
        void shouldKeepHaversineDistance_forFailedCandidatesOnly() {
            List<Position> destinations = createDestinations(3);

            // First call succeeds, second fails, third succeeds
            when(apiClient.fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car")))
                    .thenReturn(1.0)
                    .thenThrow(new RestClientException("timeout"))
                    .thenReturn(3.0);

            DistanceResult result = calculator.calculateDistances(ORIGIN, destinations, TravelMode.DRIVING);

            assertEquals(3, result.distances().size());
            // At least one distance should be the ORS value
            assertTrue(result.distances().contains(1.0) || result.distances().contains(3.0));
        }
    }

    @Nested
    class PreFiltering {

        @Test
        void shouldOnlyRefineTop15Candidates_whenMoreDestinationsProvided() {
            List<Position> destinations = createDestinations(30);

            when(apiClient.fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car")))
                    .thenReturn(2.0);

            calculator.calculateDistances(ORIGIN, destinations, TravelMode.DRIVING);

            verify(apiClient, times(15)).fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car"));
        }

        @Test
        void shouldRefineAllDestinations_whenFewerThan15() {
            List<Position> destinations = createDestinations(5);

            when(apiClient.fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car")))
                    .thenReturn(2.0);

            calculator.calculateDistances(ORIGIN, destinations, TravelMode.DRIVING);

            verify(apiClient, times(5)).fetchDistance(eq(ORIGIN), any(Position.class), eq("driving-car"));
        }
    }

    @Test
    void shouldReturnOrsStrategyName() {
        assertEquals(DistanceStrategy.ORS, calculator.getStrategy());
    }

    private static List<Position> createDestinations(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new Position(52.0 + i * 0.01, 4.0 + i * 0.01))
                .toList();
    }
}
