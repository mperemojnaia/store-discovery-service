package com.jumbo.closeststores.controller;

import com.jumbo.closeststores.controller.mapper.StoreResponseMapperImpl;
import com.jumbo.closeststores.model.StoreLocatorResult;
import com.jumbo.closeststores.model.Store;
import com.jumbo.closeststores.model.StoreWithDistance;
import com.jumbo.closeststores.model.TravelMode;
import com.jumbo.closeststores.service.StoreService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StoresApiController.class)
@Import(StoreResponseMapperImpl.class)
class StoresApiControllerIntegrationTest {

    private static final String ENDPOINT = "/api/v1/stores/closest";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreService storeService;

    private static Store sampleStore() {
        return Store.builder()
                .city("Amsterdam")
                .postalCode("1091 GZ")
                .street("Eerste Oosterparkstraat")
                .street2("")
                .street3("")
                .addressName("Jumbo Amsterdam")
                .uuid("abc-123")
                .longitude(4.9133)
                .latitude(52.3612)
                .complexNumber("32207")
                .showWarningMessage(true)
                .todayOpen("08:00")
                .locationType("Supermarkt")
                .collectionPoint(false)
                .sapStoreID("4932")
                .todayClose("21:00")
                .build();
    }

    private static StoreLocatorResult singleStoreResult() {
        return new StoreLocatorResult(
                List.of(new StoreWithDistance(sampleStore(), 1.23)),
                "haversine"
        );
    }

    @Nested
    class HappyPath {

        @Test
        void shouldReturn200WithStores_whenValidCoordinatesProvided() throws Exception {
            when(storeService.findClosestStores(eq(52.37), eq(4.90), eq(TravelMode.DRIVING)))
                    .thenReturn(singleStoreResult());

            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", "52.37")
                            .param("longitude", "4.90"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.distanceType", is("haversine")))
                    .andExpect(jsonPath("$.stores", hasSize(1)))
                    .andExpect(jsonPath("$.stores[0].addressName", is("Jumbo Amsterdam")))
                    .andExpect(jsonPath("$.stores[0].distance", is(1.23)));
        }

        @Test
        void shouldReturnAllRequiredFields_inStoreResponse() throws Exception {
            when(storeService.findClosestStores(anyDouble(), anyDouble(), any(TravelMode.class)))
                    .thenReturn(singleStoreResult());

            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", "52.37")
                            .param("longitude", "4.90"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stores[0].city", is("Amsterdam")))
                    .andExpect(jsonPath("$.stores[0].postalCode", is("1091 GZ")))
                    .andExpect(jsonPath("$.stores[0].street", is("Eerste Oosterparkstraat")))
                    .andExpect(jsonPath("$.stores[0].uuid", is("abc-123")))
                    .andExpect(jsonPath("$.stores[0].latitude", is(52.3612)))
                    .andExpect(jsonPath("$.stores[0].longitude", is(4.9133)))
                    .andExpect(jsonPath("$.stores[0].showWarningMessage", is(true)))
                    .andExpect(jsonPath("$.stores[0].collectionPoint", is(false)))
                    .andExpect(jsonPath("$.stores[0].locationType", is("Supermarkt")))
                    .andExpect(jsonPath("$.stores[0].sapStoreID", is("4932")));
        }

        @ParameterizedTest
        @ValueSource(strings = {"driving", "walking", "bicycling", "transit"})
        void shouldAcceptValidTravelModes(String mode) throws Exception {
            when(storeService.findClosestStores(anyDouble(), anyDouble(), any(TravelMode.class)))
                    .thenReturn(singleStoreResult());

            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", "52.37")
                            .param("longitude", "4.90")
                            .param("travelMode", mode))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldPassTravelModeToService() throws Exception {
            when(storeService.findClosestStores(anyDouble(), anyDouble(), eq(TravelMode.WALKING)))
                    .thenReturn(singleStoreResult());

            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", "52.37")
                            .param("longitude", "4.90")
                            .param("travelMode", "walking"))
                    .andExpect(status().isOk());

            verify(storeService).findClosestStores(52.37, 4.90, TravelMode.WALKING);
        }
    }

    @Nested
    class CoordinateValidation {

        @ParameterizedTest
        @ValueSource(strings = {"91", "-91", "100", "-100"})
        void shouldReturn400_whenLatitudeIsOutOfRange(String lat) throws Exception {
            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", lat)
                            .param("longitude", "4.90"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", containsString("latitude")));

            verifyNoInteractions(storeService);
        }

        @ParameterizedTest
        @ValueSource(strings = {"181", "-181", "360"})
        void shouldReturn400_whenLongitudeIsOutOfRange(String lng) throws Exception {
            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", "52.37")
                            .param("longitude", lng))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", containsString("longitude")));

            verifyNoInteractions(storeService);
        }

        @ParameterizedTest
        @CsvSource({"-90, -180", "90, 180", "0, 0"})
        void shouldAcceptBoundaryCoordinates(String lat, String lng) throws Exception {
            when(storeService.findClosestStores(anyDouble(), anyDouble(), eq(TravelMode.DRIVING)))
                    .thenReturn(singleStoreResult());

            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", lat)
                            .param("longitude", lng))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class MissingAndInvalidParameters {

        @Test
        void shouldReturn400_whenLatitudeIsMissing() throws Exception {
            mockMvc.perform(get(ENDPOINT)
                            .param("longitude", "4.90"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400_whenLongitudeIsMissing() throws Exception {
            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", "52.37"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400_whenLatitudeIsNotNumeric() throws Exception {
            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", "abc")
                            .param("longitude", "4.90"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("numeric")));
        }

        @ParameterizedTest
        @ValueSource(strings = {"flying", "running", "bike", ""})
        void shouldReturn400_whenTravelModeIsInvalid(String mode) throws Exception {
            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", "52.37")
                            .param("longitude", "4.90")
                            .param("travelMode", mode))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));
        }
    }

    @Nested
    class ErrorResponseFormat {

        @Test
        void shouldReturnErrorResponseWithAllFields_onBadRequest() throws Exception {
            mockMvc.perform(get(ENDPOINT)
                            .param("latitude", "999")
                            .param("longitude", "4.90"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", not(emptyString())))
                    .andExpect(jsonPath("$.timestamp", not(emptyString())));
        }
    }
}
