package com.jumbo.closeststores.service.distance.ors.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrsDirectionsResponse(List<Route> routes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(Summary summary) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(double distance, double duration) {}
}
