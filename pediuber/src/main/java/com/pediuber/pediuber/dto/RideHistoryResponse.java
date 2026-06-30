package com.pediuber.pediuber.dto;

public record RideHistoryResponse(
        Long id,
        String coreRideUuid,
        String status,
        String origin,
        String destination,
        String driverName,
        String vehicle,
        String assignedServiceId,
        Boolean delegated,
        String createdAt
) {
}
