package com.pediuber.pediuber.dto;

public record PassengerRideResponse(
        Long localRideId,
        String coreRideUuid,
        String status,
        String message
) {
}
