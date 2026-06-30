package com.pediuber.pediuber.dto;

public record PassengerRideRequest(
        String passengerName,
        LocationRequest origin,
        LocationRequest destination
) {
}