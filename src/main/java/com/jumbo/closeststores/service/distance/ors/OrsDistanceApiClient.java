package com.jumbo.closeststores.service.distance.ors;

import com.jumbo.closeststores.config.OrsProperties;
import com.jumbo.closeststores.model.Position;
import com.jumbo.closeststores.service.distance.ors.dto.OrsDirectionsResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "distance.strategy", havingValue = "ors")
public class OrsDistanceApiClient {

    private static final Logger log = LoggerFactory.getLogger(OrsDistanceApiClient.class);
    private static final double METERS_PER_KM = 1000.0;

    private final RestTemplate restTemplate;
    private final OrsProperties properties;

    public OrsDistanceApiClient(OrsProperties properties) {
        this.properties = properties;
        this.restTemplate = buildRestTemplate(properties.timeout());
    }

    @Retry(name = "orsDistanceApi")
    @CircuitBreaker(name = "orsDistanceApi")
    public double fetchDistance(Position origin, Position destination, String profile) {
        List<double[]> coordinates = List.of(
                new double[]{origin.longitude(), origin.latitude()},
                new double[]{destination.longitude(), destination.latitude()}
        );

        Map<String, Object> body = Map.of("coordinates", coordinates);

        String url = properties.baseUrl() + "/" + profile;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", properties.apiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        OrsDirectionsResponse response = restTemplate.postForObject(url, request, OrsDirectionsResponse.class);

        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            log.warn("ORS returned empty/null response for profile '{}': origin=({},{}), dest=({},{})",
                    profile, origin.latitude(), origin.longitude(),
                    destination.latitude(), destination.longitude());
            throw new RestClientException("No routes in ORS Directions response");
        }

        OrsDirectionsResponse.Route route = response.routes().get(0);
        if (route.summary() == null) {
            log.warn("ORS route missing summary for profile '{}'", profile);
            throw new RestClientException("No summary in ORS Directions route");
        }

        double distanceKm = route.summary().distance() / METERS_PER_KM;
        log.debug("ORS distance: {} km (profile={}, origin=({},{}), dest=({},{}))",
                distanceKm, profile, origin.latitude(), origin.longitude(),
                destination.latitude(), destination.longitude());

        return distanceKm;
    }

    private static RestTemplate buildRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
