package com.jumbo.closeststores.controller;

import com.jumbo.closeststores.api.StoresApi;
import com.jumbo.closeststores.api.model.StoreResponse;
import com.jumbo.closeststores.controller.mapper.StoreResponseMapper;
import com.jumbo.closeststores.model.StoreLocatorResult;
import com.jumbo.closeststores.model.TravelMode;
import com.jumbo.closeststores.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StoresApiController implements StoresApi {

    private static final Logger log = LoggerFactory.getLogger(StoresApiController.class);

    private final StoreService storeService;
    private final StoreResponseMapper storeResponseMapper;

    @Override
    public ResponseEntity<StoreResponse> getClosestStores(
            Double latitude, Double longitude, String travelMode, Integer limit) {

        log.debug("Received closest stores request: lat={}, lng={}, travelMode={}, limit={}",
                latitude, longitude, travelMode, limit);

        TravelMode mode = travelMode != null
                ? TravelMode.fromValue(travelMode)
                : TravelMode.DRIVING;

        long start = System.currentTimeMillis();

        StoreLocatorResult result =
                storeService.findClosestStores(latitude, longitude, mode, limit);

        StoreResponse response = storeResponseMapper.toStoreResponse(result);

        log.debug("Returning {} stores using {} strategy in {} ms",
                response.getStores().size(), result.distanceStrategy(),
                System.currentTimeMillis() - start);

        return ResponseEntity.ok(response);
    }
}
