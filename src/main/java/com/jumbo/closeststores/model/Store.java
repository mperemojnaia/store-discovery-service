package com.jumbo.closeststores.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record Store(
        String city,
        String postalCode,
        String street,
        String street2,
        String street3,
        String addressName,
        String uuid,
        double longitude,
        double latitude,
        String complexNumber,
        boolean showWarningMessage,
        String todayOpen,
        String locationType,
        boolean collectionPoint,
        String sapStoreID,
        String todayClose
) {}
