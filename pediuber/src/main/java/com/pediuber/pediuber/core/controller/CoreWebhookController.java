package com.pediuber.pediuber.core.controller;

import com.pediuber.pediuber.core.dto.LocationDto;
import com.pediuber.pediuber.core.dto.ProposalResponse;
import com.pediuber.pediuber.core.dto.RideAssignment;
import com.pediuber.pediuber.core.dto.RideAuctionNotification;
import com.pediuber.pediuber.core.service.LamportClockService;
import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.enums.RideStatus;
import com.pediuber.pediuber.logging.LogEvent;
import com.pediuber.pediuber.logging.LoggingService;
import com.pediuber.pediuber.policy.OverflowPolicyService;
import com.pediuber.pediuber.pool.PendingRidePool;
import com.pediuber.pediuber.repository.RideRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.pediuber.pediuber.core.client.CoreClient;
import com.pediuber.pediuber.core.dto.RideStatusUpdateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/rides")
public class CoreWebhookController {

    private final OverflowPolicyService overflowPolicyService;
    private final LamportClockService lamportClockService;
    private final RideRepository rideRepository;
    private final PendingRidePool pendingRidePool;
    private final LoggingService loggingService;
    private final CoreClient coreClient;
    private final String groupId;

    public CoreWebhookController(
            OverflowPolicyService overflowPolicyService,
            LamportClockService lamportClockService,
            RideRepository rideRepository,
            PendingRidePool pendingRidePool,
            LoggingService loggingService,
            CoreClient coreClient,
            @Value("${ridefleet.group-id}") String groupId
    ) {
        this.overflowPolicyService = overflowPolicyService;
        this.lamportClockService = lamportClockService;
        this.rideRepository = rideRepository;
        this.pendingRidePool = pendingRidePool;
        this.loggingService = loggingService;
        this.coreClient = coreClient;
        this.groupId = groupId;
    }

    @PostMapping("/incoming")
    public ResponseEntity<ProposalResponse> receiveAuctionNotification(
            @RequestBody RideAuctionNotification notification
    ) {

        long logicalTimestamp =
                lamportClockService.update(notification.getLogicalTimestamp());

        if (overflowPolicyService.isOverloaded()) {
            return ResponseEntity.noContent().build();
        }

        ProposalResponse proposal = new ProposalResponse(
                180,
                15.50,
                logicalTimestamp
        );

        return ResponseEntity.ok(proposal);
    }

    @PostMapping("/{rideUuid}/assigned")
    public ResponseEntity<Void> receiveRideAssignment(
            @PathVariable String rideUuid,
            @RequestBody RideAssignment assignment
    ) {

        long logicalTimestamp =
                lamportClockService.update(assignment.getLogicalTimestamp());

        Optional<Ride> existingRide =
                rideRepository.findByCoreRideUuid(rideUuid);

        boolean alreadyExists = existingRide.isPresent();

        Ride ride = existingRide.orElseGet(Ride::new);

        ride.setCoreRideUuid(rideUuid);
        ride.setOrigin(formatLocation(assignment.getOrigin()));
        ride.setDestination(formatLocation(assignment.getDestination()));
        ride.setExternalPassengerId(assignment.getPassengerId());
        ride.setOriginServiceId(assignment.getOriginServiceId());
        ride.setLockExpiresAt(assignment.getLockExpiresAt());
        ride.setLogicalTimestamp(logicalTimestamp);

        if (ride.getCreatedAt() == null) {
            ride.setCreatedAt(LocalDateTime.now());
        }

        if (ride.getStatus() == null) {
            ride.setStatus(RideStatus.REQUESTED);
        }

        Ride savedRide = rideRepository.save(ride);

        if (!alreadyExists) {
            pendingRidePool.addRide(savedRide);
        }

        loggingService.info(
                new LogEvent(
                        LocalDateTime.now().toString(),
                        "RIDE_ASSIGNED_BY_CORE",
                        savedRide.getId(),
                        "PediUber",
                        null,
                        savedRide.getStatus().name(),
                        null
                )
        );

        try {

            long confirmTimestamp = lamportClockService.tick();

            RideStatusUpdateRequest statusUpdateRequest =
                    new RideStatusUpdateRequest(
                            "confirm",
                            groupId,
                            confirmTimestamp
                    );

            coreClient.updateRideStatus(
                    rideUuid,
                    statusUpdateRequest
            );

            savedRide.setStatus(RideStatus.CONFIRMED);
            savedRide.setLogicalTimestamp(confirmTimestamp);

            rideRepository.save(savedRide);

            loggingService.info(
                    new LogEvent(
                            LocalDateTime.now().toString(),
                            "RIDE_CONFIRMED_TO_CORE",
                            savedRide.getId(),
                            "PediUber",
                            RideStatus.REQUESTED.name(),
                            RideStatus.CONFIRMED.name(),
                            null
                    )
            );

        } catch (RestClientException exception) {

            loggingService.warn(
                    new LogEvent(
                            LocalDateTime.now().toString(),
                            "CORE_CONFIRM_FAILED",
                            savedRide.getId(),
                            "PediUber",
                            savedRide.getStatus().name(),
                            savedRide.getStatus().name(),
                            null
                    )
            );
        }

        return ResponseEntity.ok().build();
    }

    private String formatLocation(LocationDto location) {

        if (location == null) {
            return "Local não informado";
        }

        StringBuilder builder = new StringBuilder();

        if (location.getStreet() != null && !location.getStreet().isBlank()) {
            builder.append(location.getStreet());
        }

        if (location.getNumber() != null && !location.getNumber().isBlank()) {
            builder.append(", ").append(location.getNumber());
        }

        if (location.getCity() != null && !location.getCity().isBlank()) {
            builder.append(" - ").append(location.getCity());
        }

        if (location.getState() != null && !location.getState().isBlank()) {
            builder.append("/").append(location.getState());
        }

        if (builder.length() == 0) {
            return location.getLat() + "," + location.getLng();
        }

        return builder.toString();
    }
}