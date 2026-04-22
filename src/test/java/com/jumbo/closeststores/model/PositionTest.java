package com.jumbo.closeststores.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PositionTest {

    @Test
    void shouldCreatePosition_whenCoordinatesAreValid() {
        Position position = new Position(52.3676, 4.9041);

        assertAll(
                () -> assertEquals(52.3676, position.latitude()),
                () -> assertEquals(4.9041, position.longitude())
        );
    }

    @ParameterizedTest
    @CsvSource({"-90, 0", "90, 0", "0, -180", "0, 180", "0, 0", "-90, -180", "90, 180"})
    void shouldAcceptBoundaryCoordinates(double lat, double lng) {
        assertDoesNotThrow(() -> new Position(lat, lng));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-90.1, 91, 100, -200, Double.MAX_VALUE})
    void shouldThrowException_whenLatitudeIsOutOfRange(double invalidLat) {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Position(invalidLat, 0)
        );
        assertTrue(ex.getMessage().contains("Latitude"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-180.1, 181, 360, -360, Double.MAX_VALUE})
    void shouldThrowException_whenLongitudeIsOutOfRange(double invalidLng) {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Position(0, invalidLng)
        );
        assertTrue(ex.getMessage().contains("Longitude"));
    }
}
