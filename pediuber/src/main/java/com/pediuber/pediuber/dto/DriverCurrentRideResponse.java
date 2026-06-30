package com.pediuber.pediuber.dto;

import com.pediuber.pediuber.entity.Driver;
import com.pediuber.pediuber.entity.Ride;

public record DriverCurrentRideResponse(
        Long driverId,
        String driverName,
        Boolean driverAvailable,
        Boolean hasCurrentRide,
        Long rideId,
        String rideStatus,
        String origin,
        String destination,
        String coreRideUuid
) {

    public static DriverCurrentRideResponse noRide(Driver driver) {
        return new DriverCurrentRideResponse(
                driver.getId(),
                driver.getName(),
                driver.getAvailable(),
                false,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static DriverCurrentRideResponse fromRide(Driver driver, Ride ride) {
        return new DriverCurrentRideResponse(
                driver.getId(),
                driver.getName(),
                driver.getAvailable(),
                true,
                ride.getId(),
                ride.getStatus().name(),
                ride.getOrigin(),
                ride.getDestination(),
                ride.getCoreRideUuid()
        );
    }
}