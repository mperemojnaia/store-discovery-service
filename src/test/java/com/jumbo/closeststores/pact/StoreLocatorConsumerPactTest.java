package com.jumbo.closeststores.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "StoreLocatorAPI")
class StoreLocatorConsumerPactTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Pact(consumer = "StoreLocatorClient")
    V4Pact closestStoresHappyPath(PactDslWithProvider builder) {
        return builder
                .given("stores exist")
                .uponReceiving("a request for closest stores with valid coordinates")
                .path("/api/v1/stores/closest")
                .method("GET")
                .query("latitude=52.3676&longitude=4.9041")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringMatcher("distanceType", "haversine|google", "haversine")
                        .minArrayLike("stores", 1)
                            .stringType("city", "Amsterdam")
                            .stringType("postalCode", "1091 GZ")
                            .stringType("street", "Eerste Oosterparkstraat")
                            .stringType("addressName", "Jumbo Amsterdam")
                            .stringType("uuid", "abc-123")
                            .decimalType("latitude", 52.3612)
                            .decimalType("longitude", 4.9133)
                            .decimalType("distance", 1.23)
                            .stringType("complexNumber", "32207")
                            .booleanType("showWarningMessage", true)
                            .stringType("todayOpen", "08:00")
                            .stringType("todayClose", "21:00")
                            .stringType("locationType", "Supermarkt")
                            .booleanType("collectionPoint", false)
                            .stringType("sapStoreID", "4932")
                        .closeArray())
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "StoreLocatorClient")
    V4Pact closestStoresWithTravelMode(PactDslWithProvider builder) {
        return builder
                .given("stores exist")
                .uponReceiving("a request for closest stores with travel mode")
                .path("/api/v1/stores/closest")
                .method("GET")
                .query("latitude=52.3676&longitude=4.9041&travelMode=driving")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringMatcher("distanceType", "haversine|google", "haversine")
                        .eachLike("stores", 1)
                            .decimalType("distance", 2.5)
                            .stringType("addressName", "Jumbo Store")
                        .closeArray())
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "StoreLocatorClient")
    V4Pact invalidLatitude(PactDslWithProvider builder) {
        return builder
                .given("any state")
                .uponReceiving("a request with invalid latitude")
                .path("/api/v1/stores/closest")
                .method("GET")
                .query("latitude=999&longitude=4.90")
                .willRespondWith()
                .status(400)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .integerType("status", 400)
                        .stringType("message", "Latitude must be between -90.0 and 90.0")
                        .stringMatcher("timestamp", "\\d{4}-\\d{2}-\\d{2}T.*", "2026-01-01T00:00:00Z"))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "closestStoresHappyPath")
    void shouldReturnClosestStores_whenValidCoordinatesProvided(MockServer mockServer) {
        String url = mockServer.getUrl() + "/api/v1/stores/closest?latitude=52.3676&longitude=4.9041";

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        assertNotNull(response);
        assertAll(
                () -> assertNotNull(response.get("distanceType")),
                () -> assertNotNull(response.get("stores")),
                () -> assertInstanceOf(java.util.List.class, response.get("stores"))
        );
    }

    @Test
    @PactTestFor(pactMethod = "closestStoresWithTravelMode")
    void shouldReturnStores_whenTravelModeIsProvided(MockServer mockServer) {
        String url = mockServer.getUrl()
                + "/api/v1/stores/closest?latitude=52.3676&longitude=4.9041&travelMode=driving";

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        assertNotNull(response);
        assertNotNull(response.get("stores"));
    }

    @Test
    @PactTestFor(pactMethod = "invalidLatitude")
    void shouldReturn400_whenLatitudeIsInvalid(MockServer mockServer) {
        String url = mockServer.getUrl() + "/api/v1/stores/closest?latitude=999&longitude=4.90";

        HttpClientErrorException ex = assertThrows(
                HttpClientErrorException.class,
                () -> restTemplate.getForObject(url, Map.class)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
