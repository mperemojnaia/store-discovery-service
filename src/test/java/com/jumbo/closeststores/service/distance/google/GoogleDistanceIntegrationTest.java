package com.jumbo.closeststores.service.distance.google;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "distance.strategy=google",
        "google.maps.api-key=fake-test-key",
        "google.maps.base-url=http://localhost:9999/maps/api/distancematrix/json",
        "resilience4j.circuitbreaker.instances.googleDistanceApi.sliding-window-size=100",
        "resilience4j.circuitbreaker.instances.googleDistanceApi.failure-rate-threshold=100",
        "resilience4j.retry.instances.googleDistanceApi.max-attempts=1"
})
@AutoConfigureMockMvc
@WireMockTest(httpPort = 9999)
class GoogleDistanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String GOOGLE_PATH = "/maps/api/distancematrix/json";
    private static final String ENDPOINT = "/api/v1/stores/closest";

    @BeforeEach
    void resetStubs() {
        removeAllMappings();
    }

    @Test
    void shouldReturnGoogleDistances_whenApiRespondsSuccessfully() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo(GOOGLE_PATH))
                .willReturn(okJson(successResponse())));

        mockMvc.perform(get(ENDPOINT)
                        .param("latitude", "52.3676")
                        .param("longitude", "4.9041")
                        .param("travelMode", "driving"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceType", is("google")))
                .andExpect(jsonPath("$.stores", hasSize(5)))
                .andExpect(jsonPath("$.stores[0].distance", notNullValue()));
    }

    @Test
    void shouldFallBackToHaversine_whenGoogleApiReturnsRequestDenied() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo(GOOGLE_PATH))
                .willReturn(okJson("{\"status\": \"REQUEST_DENIED\"}")));

        mockMvc.perform(get(ENDPOINT)
                        .param("latitude", "52.3676")
                        .param("longitude", "4.9041"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceType", is("haversine")))
                .andExpect(jsonPath("$.stores", hasSize(5)));
    }

    @Test
    void shouldFallBackToHaversine_whenGoogleApiReturnsServerError() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo(GOOGLE_PATH))
                .willReturn(serverError()));

        mockMvc.perform(get(ENDPOINT)
                        .param("latitude", "52.3676")
                        .param("longitude", "4.9041"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceType", is("haversine")))
                .andExpect(jsonPath("$.stores", hasSize(5)));
    }

    @Test
    void shouldFallBackToHaversine_whenGoogleApiReturnsOverQueryLimit() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo(GOOGLE_PATH))
                .willReturn(okJson("{\"status\": \"OVER_QUERY_LIMIT\"}")));

        mockMvc.perform(get(ENDPOINT)
                        .param("latitude", "52.3676")
                        .param("longitude", "4.9041"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceType", is("haversine")))
                .andExpect(jsonPath("$.stores", hasSize(5)));
    }

    private String successResponse() {
        StringBuilder elements = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            if (i > 0) elements.append(",");
            int meters = 1000 + i * 500;
            elements.append(String.format(
                    "{\"status\":\"OK\",\"distance\":{\"value\":%d,\"text\":\"%s km\"}}",
                    meters, meters / 1000.0));
        }
        return "{\"status\":\"OK\",\"rows\":[{\"elements\":[" + elements + "]}]}";
    }
}
