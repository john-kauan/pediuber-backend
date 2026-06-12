package com.pediuber.pediuber.core.service;

import com.pediuber.pediuber.core.client.CoreClient;
import com.pediuber.pediuber.core.dto.LocationDto;
import com.pediuber.pediuber.core.dto.RideAccepted;
import com.pediuber.pediuber.core.dto.RideRequestToCore;
import com.pediuber.pediuber.entity.Ride;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CoreDelegationService {

    private final CoreClient coreClient;
    private final LamportClockService lamportClockService;
    private final String groupId;

    public CoreDelegationService(
            CoreClient coreClient,
            LamportClockService lamportClockService,
            @Value("${ridefleet.group-id}") String groupId
    ) {
        this.coreClient = coreClient;
        this.lamportClockService = lamportClockService;
        this.groupId = groupId;
    }

    public RideAccepted delegateRide(Ride ride) {

        long logicalTimestamp = lamportClockService.tick();

        RideRequestToCore request = new RideRequestToCore(
                groupId,
                resolvePassengerId(ride),
                buildLocation(ride.getOrigin()),
                buildLocation(ride.getDestination()),
                logicalTimestamp,
                10
        );

        return coreClient.createRide(request);
    }

    private String resolvePassengerId(Ride ride) {

        if (ride.getPassenger() != null &&
                ride.getPassenger().getId() != null) {

            return String.valueOf(ride.getPassenger().getId());
        }

        if (ride.getExternalPassengerId() != null &&
                !ride.getExternalPassengerId().isBlank()) {

            return ride.getExternalPassengerId();
        }

        if (ride.getId() != null) {
            return "pediuber-passenger-" + ride.getId();
        }

        return "pediuber-passenger-unknown";
    }

    private LocationDto buildLocation(String description) {

        return new LocationDto(
                0.0,
                0.0,
                description,
                null,
                null,
                null
        );
    }
}