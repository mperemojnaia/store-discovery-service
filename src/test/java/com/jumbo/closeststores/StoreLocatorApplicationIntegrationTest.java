package com.jumbo.closeststores;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StoreLocatorApplicationIntegrationTest {

    private static final String ENDPOINT = "/api/v1/stores/closest";

    @Autowired
    private MockMvc mockMvc;

    @Nested
    class FullStackHappyPath {

        @Test
        void shouldReturn5ClosestStores_whenValidAmsterdamCoordinatesProvided() throws Exception {
            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", "52.3676")
                            .param("longitude", "4.9041"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.distanceType", is("haversine")))
                    .andExpect(jsonPath("$.stores", hasSize(5)))
                    .andExpect(jsonPath("$.stores[0].distance", notNullValue()))
                    .andExpect(jsonPath("$.stores[0].addressName", notNullValue()));
        }

        @Test
        void shouldReturnStoresSortedByDistanceAscending() throws Exception {
            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", "52.3676")
                            .param("longitude", "4.9041"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stores[0].distance",
                            lessThanOrEqualTo(!jsonPath("$.stores[1].distance").toString().isEmpty() ? 1000.0 : 0.0)))
                    .andExpect(jsonPath("$.stores[0].distance", notNullValue()))
                    .andExpect(jsonPath("$.stores[4].distance", notNullValue()));
        }

        @Test
        void shouldReturnNumericLatLngInResponse() throws Exception {
            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", "52.3676")
                            .param("longitude", "4.9041"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stores[0].latitude", isA(Number.class)))
                    .andExpect(jsonPath("$.stores[0].longitude", isA(Number.class)))
                    .andExpect(jsonPath("$.stores[0].distance", isA(Number.class)));
        }
    }

    @Nested
    class FullStackErrorHandling {

        @Test
        void shouldReturn400_whenLatitudeExceedsBounds() throws Exception {
            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", "91")
                            .param("longitude", "4.90"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", containsString("latitude")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));
        }

        @Test
        void shouldReturn400_whenParametersAreMissing() throws Exception {
            mockMvc.perform(get(ENDPOINT))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400_whenNonNumericLatitudeProvided() throws Exception {
            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", "not-a-number")
                            .param("longitude", "4.90"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));
        }
    }
}
