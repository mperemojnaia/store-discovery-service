package com.jumbo.closeststores.service.distance.haversine;

import com.jumbo.closeststores.model.DistanceResult;
import com.jumbo.closeststores.model.DistanceStrategy;
import com.jumbo.closeststores.model.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HaversineDistanceCalculatorTest {

    private HaversineDistanceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new HaversineDistanceCalculator();
    }

    @Test
    void shouldReturnApproximately57Km_whenCalculatingAmsterdamToRotterdam() {
        Position amsterdam = new Position(52.3676, 4.9041);
        Position rotterdam = new Position(51.9225, 4.4792);

        double distance = calculator.calculate(amsterdam, rotterdam);

        assertTrue(distance > 55 && distance < 59,
                "Expected ~57km, got " + distance);
    }

    @Test
    void shouldReturnZero_whenBothPositionsAreIdentical() {
        Position pos = new Position(52.3676, 4.9041);

        assertEquals(0.0, calculator.calculate(pos, pos));
    }

    @Test
    void shouldReturnSymmetricDistance_whenSwappingOriginAndDestination() {
        Position a = new Position(52.3676, 4.9041);
        Position b = new Position(51.9225, 4.4792);

        assertEquals(calculator.calculate(a, b), calculator.calculate(b, a), 1e-9);
    }

    @Test
    void shouldReturnCorrectNumberOfDistances_whenCalculatingBatch() {
        Position origin = new Position(52.3676, 4.9041);
        List<Position> destinations = List.of(
                new Position(51.9225, 4.4792),
                new Position(52.0907, 5.1214),
                new Position(51.4416, 5.4697)
        );

        DistanceResult result = calculator.calculateDistances(origin, destinations, null);

        assertAll(
                () -> assertEquals(3, result.distances().size()),
                () -> assertEquals(DistanceStrategy.HAVERSINE, result.strategyUsed())
        );
    }

    @Test
    void shouldReturnEmptyDistances_whenDestinationListIsEmpty() {
        DistanceResult result = calculator.calculateDistances(
                new Position(0, 0), List.of(), null);

        assertAll(
                () -> assertTrue(result.distances().isEmpty()),
                () -> assertEquals(DistanceStrategy.HAVERSINE, result.strategyUsed())
        );
    }

    @Test
    void shouldReturnHaversine_asStrategy() {
        assertEquals(DistanceStrategy.HAVERSINE, calculator.getStrategy());
    }
}
