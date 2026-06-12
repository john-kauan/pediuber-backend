package com.pediuber.pediuber.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestToCore {

    private String originServiceId;

    private String passengerId;

    private LocationDto origin;

    private LocationDto destination;

    private long logicalTimestamp;

    private int auctionTimeoutSeconds;
}