package com.pediuber.pediuber.core.service;

import com.pediuber.pediuber.core.client.CoreClient;
import com.pediuber.pediuber.core.dto.RideStatusUpdateRequest;
import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.enums.RideStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CoreRideStatusService {

    private final CoreClient coreClient;
    private final LamportClockService lamportClockService;
    private final String groupId;

    public CoreRideStatusService(
            CoreClient coreClient,
            LamportClockService lamportClockService,
            @Value("${ridefleet.group-id}") String groupId
    ) {
        this.coreClient = coreClient;
        this.lamportClockService = lamportClockService;
        this.groupId = groupId;
    }

    public void notifyStatusChange(Ride ride, RideStatus newStatus) {

        if (ride.getCoreRideUuid() == null ||
                ride.getCoreRideUuid().isBlank()) {
            return;
        }

        String coreState = mapToCoreState(newStatus);

        if (coreState == null) {
            return;
        }

        long logicalTimestamp = lamportClockService.nextAfter(
                ride.getLogicalTimestamp()
        );

        RideStatusUpdateRequest request = new RideStatusUpdateRequest(
                coreState,
                groupId,
                logicalTimestamp
        );

        coreClient.updateRideStatus(
                ride.getCoreRideUuid(),
                request
        );

        ride.setLogicalTimestamp(logicalTimestamp);
    }

    private String mapToCoreState(RideStatus status) {

        return switch (status) {
            case CONFIRMED -> "confirm";
            case IN_TRANSIT -> "in_transit";
            case COMPLETED -> "complete";
            case CANCELLED -> "cancelled";
            case COMPENSATING -> "compensating";
            default -> null;
        };
    }
}