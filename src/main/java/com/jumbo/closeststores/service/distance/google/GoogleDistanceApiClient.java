package com.jumbo.closeststores.service.distance.google;

import com.jumbo.closeststores.config.GoogleMapsProperties;
import com.jumbo.closeststores.model.Position;
import com.jumbo.closeststores.service.distance.google.dto.DistanceMatrixResponse;
import com.jumbo.closeststores.service.distance.google.dto.GoogleApiStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "distance.strategy", havingValue = "google")
public class GoogleDistanceApiClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleDistanceApiClient.class);
    private static final double METERS_PER_KM = 1000.0;

    private final RestClient restClient;
    private final GoogleMapsProperties properties;

    public GoogleDistanceApiClient(GoogleMapsProperties properties) {
        this.properties = properties;
        this.restClient = buildRestClient(properties.timeout());
    }

    @Retry(name = "googleDistanceApi")
    @CircuitBreaker(name = "googleDistanceApi")
    public List<Double> fetchDistances(Position origin, List<Position> destinations, String mode) {
        log.debug("Fetching Google distances: mode={}, origin=({},{}), destinations={}",
                mode, origin.latitude(), origin.longitude(), destinations.size());

        String originsParam = origin.latitude() + "," + origin.longitude();
        String destinationsParam = destinations.stream()
                .map(p -> p.latitude() + "," + p.longitude())
                .collect(Collectors.joining("|"));

        URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .queryParam("origins", originsParam)
                .queryParam("destinations", destinationsParam)
                .queryParam("mode", mode)
                .queryParam("key", properties.apiKey())
                .build()
                .toUri();

        DistanceMatrixResponse response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(DistanceMatrixResponse.class);

        if (response == null) {
            throw new RestClientException("Null response from Google Distance Matrix API");
        }

        validateTopLevelStatus(response.status());

        if (response.rows() == null || response.rows().isEmpty()) {
            throw new RestClientException("No rows in Google Distance Matrix API response");
        }

        List<DistanceMatrixResponse.Element> elements = response.rows().get(0).elements();
        if (elements == null) {
            throw new RestClientException("No elements in Google Distance Matrix API response");
        }

        List<Double> distances = new ArrayList<>(elements.size());
        for (DistanceMatrixResponse.Element element : elements) {
            if (!GoogleApiStatus.isOk(element.status())) {
                log.warn("Element status not OK: {}", element.status());
                throw new RestClientException("Element status: " + element.status());
            }
            if (element.distance() == null) {
                throw new RestClientException("Missing distance data for element with status: " + element.status());
            }
            distances.add(element.distance().value() / METERS_PER_KM);
        }

        return distances;
    }

    private void validateTopLevelStatus(String status) {
        if (GoogleApiStatus.isOk(status)) {
            return;
        }
        if (GoogleApiStatus.OVER_QUERY_LIMIT.getValue().equals(status)) {
            log.warn("Google Distance Matrix API returned OVER_QUERY_LIMIT");
            throw new RestClientException(GoogleApiStatus.OVER_QUERY_LIMIT.getValue());
        }
        if (GoogleApiStatus.REQUEST_DENIED.getValue().equals(status)) {
            log.warn("Google Distance Matrix API returned REQUEST_DENIED — check API key configuration");
            throw new RestClientException(GoogleApiStatus.REQUEST_DENIED.getValue());
        }
        log.warn("Google Distance Matrix API returned unexpected status: {}", status);
        throw new RestClientException("Unexpected API status: " + status);
    }

    private static RestClient buildRestClient(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
