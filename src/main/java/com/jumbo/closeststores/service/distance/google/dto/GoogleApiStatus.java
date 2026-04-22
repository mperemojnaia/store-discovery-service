package com.jumbo.closeststores.service.distance.google.dto;

import lombok.Getter;

@Getter
public enum GoogleApiStatus {
    OK("OK"),
    OVER_QUERY_LIMIT("OVER_QUERY_LIMIT"),
    REQUEST_DENIED("REQUEST_DENIED");

    private final String value;

    GoogleApiStatus(String value) {
        this.value = value;
    }

    public static boolean isOk(String status) {
        return OK.value.equals(status);
    }
}
