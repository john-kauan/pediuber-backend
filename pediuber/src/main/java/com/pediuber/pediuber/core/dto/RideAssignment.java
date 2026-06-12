package com.pediuber.pediuber.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RideAssignment {

    private String rideUuid;

    private LocationDto origin;

    private LocationDto destination;

    private String passengerId;

    private String originServiceId;

    private long logicalTimestamp;

    private String lockExpiresAt;

}
