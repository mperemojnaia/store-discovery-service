package com.jumbo.closeststores.service.distance.ors;

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
        "distance.strategy=ors",
        "ors.api-key=fake-test-key",
        "ors.base-url=http://localhost:9998/v2/directions",
        "resilience4j.circuitbreaker.instances.orsDistanceApi.sliding-window-size=100",
        "resilience4j.circuitbreaker.instances.orsDistanceApi.failure-rate-threshold=100",
        "resilience4j.retry.instances.orsDistanceApi.max-attempts=1"
})
@AutoConfigureMockMvc
@WireMockTest(httpPort = 9998)
class OrsDistanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String ENDPOINT = "/api/v1/stores/closest";

    @BeforeEach
    void resetStubs() {
        removeAllMappings();
    }

    @Test
    void shouldReturnOrsDistances_whenApiRespondsSuccessfully() throws Exception {
        stubFor(post(urlPathMatching("/v2/directions/.*"))
                .willReturn(okJson(successResponse(1500.0))));

        mockMvc.perform(get(ENDPOINT)
                        .param("latitude", "52.3676")
                        .param("longitude", "4.9041")
                        .param("travelMode", "driving"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceType", is("ors")))
                .andExpect(jsonPath("$.stores", hasSize(5)))
                .andExpect(jsonPath("$.stores[0].distance", notNullValue()));
    }

    @Test
    void shouldFallBackToHaversine_whenOrsReturnsServerError() throws Exception {
        stubFor(post(urlPathMatching("/v2/directions/.*"))
                .willReturn(serverError()));

        mockMvc.perform(get(ENDPOINT)
                        .param("latitude", "52.3676")
                        .param("longitude", "4.9041"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceType", is("haversine")))
                .andExpect(jsonPath("$.stores", hasSize(5)));
    }

    @Test
    void shouldFallBackToHaversine_whenOrsReturns403() throws Exception {
        stubFor(post(urlPathMatching("/v2/directions/.*"))
                .willReturn(forbidden()));

        mockMvc.perform(get(ENDPOINT)
                        .param("latitude", "52.3676")
                        .param("longitude", "4.9041"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceType", is("haversine")))
                .andExpect(jsonPath("$.stores", hasSize(5)));
    }

    @Test
    void shouldUseCorrectOrsProfile_forWalkingMode() throws Exception {
        stubFor(post(urlPathEqualTo("/v2/directions/foot-walking"))
                .willReturn(okJson(successResponse(800.0))));
        // Fallback for any other profile
        stubFor(post(urlPathMatching("/v2/directions/(?!foot-walking).*"))
                .willReturn(serverError()));

        mockMvc.perform(get(ENDPOINT)
                        .param("latitude", "52.3676")
                        .param("longitude", "4.9041")
                        .param("travelMode", "walking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceType", is("ors")));

        verify(postRequestedFor(urlPathEqualTo("/v2/directions/foot-walking")));
    }

    private String successResponse(double distanceMeters) {
        return """
                {
                  "routes": [{
                    "summary": {
                      "distance": %s,
                      "duration": 300.0
                    }
                  }]
                }
                """.formatted(distanceMeters);
    }
}
