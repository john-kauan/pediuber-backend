package com.pediuber.pediuber.dto;

public record RideTrackingResponse(
        Long localRideId,
        String coreRideUuid,
        String localStatus,
        String displayStatus,
        String origin,
        String destination,
        String assignedServiceId,
        Boolean delegated,
        Long driverId,
        String driverName,
        String vehicle,
        Integer etaSeconds,
        Integer progressPercent,
        Boolean canStart,
        Boolean canComplete
) {
}