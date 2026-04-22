package com.jumbo.closeststores.service.distance.google.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DistanceMatrixResponse(String status, List<Row> rows) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Row(List<Element> elements) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Element(String status, Distance distance) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Distance(int value, String text) {}
}
