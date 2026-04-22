package com.jumbo.closeststores.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoogleMapsPropertiesTest {

    @Test
    void shouldUseProvidedValues_whenAllAreValid() {
        GoogleMapsProperties props = new GoogleMapsProperties("key", "https://custom.api", 3000, 10);

        assertAll(
                () -> assertEquals("key", props.apiKey()),
                () -> assertEquals("https://custom.api", props.baseUrl()),
                () -> assertEquals(3000, props.timeout()),
                () -> assertEquals(10, props.maxDestinationsPerRequest())
        );
    }

    @Test
    void shouldApplyDefaultBaseUrl_whenNullProvided() {
        GoogleMapsProperties props = new GoogleMapsProperties("key", null, 5000, 25);

        assertTrue(props.baseUrl().contains("googleapis.com"));
    }

    @Test
    void shouldApplyDefaultBaseUrl_whenBlankProvided() {
        GoogleMapsProperties props = new GoogleMapsProperties("key", "  ", 5000, 25);

        assertTrue(props.baseUrl().contains("googleapis.com"));
    }

    @Test
    void shouldApplyDefaultTimeout_whenZeroOrNegativeProvided() {
        assertAll(
                () -> assertEquals(5000, new GoogleMapsProperties("k", null, 0, 25).timeout()),
                () -> assertEquals(5000, new GoogleMapsProperties("k", null, -1, 25).timeout())
        );
    }

    @Test
    void shouldApplyDefaultMaxDestinations_whenZeroOrNegativeProvided() {
        assertAll(
                () -> assertEquals(25, new GoogleMapsProperties("k", null, 5000, 0).maxDestinationsPerRequest()),
                () -> assertEquals(25, new GoogleMapsProperties("k", null, 5000, -5).maxDestinationsPerRequest())
        );
    }
}
