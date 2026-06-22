package com.pediuber.pediuber.core.dto;

public record RideStatusUpdateRequest(
        String newState,
        String serviceId,
        Long logicalTimestamp
) {
}