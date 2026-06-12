package com.pediuber.pediuber.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RideAuctionNotification {

    private String rideUuid;

    private LocationDto origin;

    private LocationDto destination;

    private String originServiceId;

    private String passengerId;

    private long logicalTimestamp;

    private String auctionDeadline;

}
