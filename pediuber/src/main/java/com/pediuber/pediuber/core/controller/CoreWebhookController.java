package com.pediuber.pediuber.core.controller;

import com.pediuber.pediuber.core.dto.LocationDto;
import com.pediuber.pediuber.core.dto.ProposalResponse;
import com.pediuber.pediuber.core.dto.RideAssignment;
import com.pediuber.pediuber.core.dto.RideAuctionNotification;
import com.pediuber.pediuber.core.service.LamportClockService;
import com.pediuber.pediuber.entity.Driver;
import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.enums.RideStatus;
import com.pediuber.pediuber.logging.LogEvent;
import com.pediuber.pediuber.logging.LoggingService;
import com.pediuber.pediuber.policy.OverflowPolicyService;
import com.pediuber.pediuber.pool.PendingRidePool;
import com.pediuber.pediuber.repository.DriverRepository;
import com.pediuber.pediuber.repository.RideRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.pediuber.pediuber.core.client.CoreClient;
import com.pediuber.pediuber.core.dto.RideStatusUpdateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientException;
import com.pediuber.pediuber.service.RideService;
import com.pediuber.pediuber.metrics.PediUberMetricsService;
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
    private final DriverRepository driverRepository;
    private final RideService rideService;
    private final PediUberMetricsService metricsService;

    public CoreWebhookController(
            OverflowPolicyService overflowPolicyService,
            LamportClockService lamportClockService,
            RideRepository rideRepository,
            PendingRidePool pendingRidePool,
            LoggingService loggingService,
            DriverRepository driverRepository,
            CoreClient coreClient,
            RideService rideService,
            PediUberMetricsService metricsService,
            @Value("${ridefleet.group-id}") String groupId
    ) {
        this.overflowPolicyService = overflowPolicyService;
        this.lamportClockService = lamportClockService;
        this.rideRepository = rideRepository;
        this.pendingRidePool = pendingRidePool;
        this.loggingService = loggingService;
        this.coreClient = coreClient;
        this.rideService = rideService;
        this.metricsService = metricsService;
        this.groupId = groupId;
        this.driverRepository = driverRepository;
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

        if (isAlreadyConfirmedOrBeyond(savedRide)) {
            return ResponseEntity.ok().build();
        }

        if (!alreadyExists) {
            pendingRidePool.addRide(savedRide);
            metricsService.incrementDelegatedInRide();
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

        savedRide = assignAvailableDriverIfPossible(savedRide);

        if (savedRide.getDriver() == null) {
            notifyCompensationToCore(savedRide);
            return ResponseEntity.ok().build();
        }

        try {

            Ride confirmedRide = rideService.confirmRide(savedRide.getId());

            loggingService.info(
                    new LogEvent(
                            LocalDateTime.now().toString(),
                            "RIDE_CONFIRMED_TO_CORE",
                            confirmedRide.getId(),
                            "PediUber",
                            RideStatus.MATCHED.name(),
                            RideStatus.CONFIRMED.name(),
                            null
                    )
            );

        } catch (RuntimeException exception) {

            loggingService.warn(
                    new LogEvent(
                            LocalDateTime.now().toString(),
                            "CORE_CONFIRM_FAILED",
                            savedRide.getId(),
                            "PediUber",
                            savedRide.getStatus().name(),
                            savedRide.getStatus().name(),
                            exception.getMessage()
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

    private Ride assignAvailableDriverIfPossible(Ride ride) {

        if (ride.getDriver() != null) {
            return ride;
        }

        if (isAlreadyConfirmedOrBeyond(ride)) {
            return ride;
        }

        Driver driver = driverRepository
                .findFirstByAvailableTrue()
                .orElse(null);

        if (driver == null) {

            loggingService.warn(
                    new LogEvent(
                            LocalDateTime.now().toString(),
                            "NO_DRIVER_AVAILABLE_FOR_CORE_RIDE",
                            ride.getId(),
                            "PediUber",
                            ride.getStatus().name(),
                            ride.getStatus().name(),
                            null
                    )
            );

            return ride;
        }

        RideStatus previousStatus = ride.getStatus();

        driver.setAvailable(false);
        driverRepository.save(driver);

        ride.setDriver(driver);
        ride.setStatus(RideStatus.MATCHED);

        Ride savedRide = rideRepository.save(ride);

        loggingService.info(
                new LogEvent(
                        LocalDateTime.now().toString(),
                        "DRIVER_ASSIGNED_TO_CORE_RIDE",
                        savedRide.getId(),
                        "PediUber",
                        previousStatus.name(),
                        savedRide.getStatus().name(),
                        null
                )
        );

        return savedRide;
    }

    private void notifyCompensationToCore(Ride ride) {

        try {

            long compensationTimestamp = lamportClockService.nextAfter(
                    ride.getLogicalTimestamp()
            );

            RideStatusUpdateRequest statusUpdateRequest =
                    new RideStatusUpdateRequest(
                            "compensating",
                            groupId,
                            compensationTimestamp
                    );

            coreClient.updateRideStatus(
                    ride.getCoreRideUuid(),
                    statusUpdateRequest
            );

            ride.setStatus(RideStatus.COMPENSATING);
            ride.setLogicalTimestamp(compensationTimestamp);

            rideRepository.save(ride);

            loggingService.warn(
                    new LogEvent(
                            LocalDateTime.now().toString(),
                            "RIDE_COMPENSATING_TO_CORE",
                            ride.getId(),
                            "PediUber",
                            RideStatus.MATCHED.name(),
                            RideStatus.COMPENSATING.name(),
                            "Sem motorista disponível para atender corrida atribuída pelo Core"
                    )
            );

        } catch (RestClientException exception) {

            loggingService.warn(
                    new LogEvent(
                            LocalDateTime.now().toString(),
                            "CORE_COMPENSATION_FAILED",
                            ride.getId(),
                            "PediUber",
                            ride.getStatus().name(),
                            ride.getStatus().name(),
                            exception.getMessage()
                    )
            );
        }
    }

    private boolean isAlreadyConfirmedOrBeyond(Ride ride) {

        return ride.getStatus() == RideStatus.CONFIRMED
                || ride.getStatus() == RideStatus.IN_TRANSIT
                || ride.getStatus() == RideStatus.COMPLETED
                || ride.getStatus() == RideStatus.CANCELLED
                || ride.getStatus() == RideStatus.COMPENSATING;
    }
}