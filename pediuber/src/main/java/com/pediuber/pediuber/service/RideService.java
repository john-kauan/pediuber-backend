package com.pediuber.pediuber.service;

import com.pediuber.pediuber.entity.Driver;
import com.pediuber.pediuber.entity.Passenger;
import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.enums.RideStatus;
import com.pediuber.pediuber.logging.LogEvent;
import com.pediuber.pediuber.pool.PendingRidePool;
import com.pediuber.pediuber.repository.DriverRepository;
import com.pediuber.pediuber.repository.PassengerRepository;
import com.pediuber.pediuber.repository.RideRepository;
import org.springframework.stereotype.Service;
import com.pediuber.pediuber.logging.LoggingService;
import com.pediuber.pediuber.policy.OverflowPolicyService;
import com.pediuber.pediuber.rabbitmq.RideProducer;
import com.pediuber.pediuber.core.dto.RideAccepted;
import com.pediuber.pediuber.core.service.CoreDelegationService;
import org.springframework.web.client.RestClientException;
import com.pediuber.pediuber.core.service.CoreRideStatusService;
import org.springframework.web.client.RestClientResponseException;
import java.time.LocalDateTime;


@Service
public class RideService {

    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final PassengerRepository passengerRepository;
    private final PendingRidePool pendingRidePool;
    private final LoggingService loggingService;
    private final OverflowPolicyService overflowPolicyService;
    private final RideProducer rideProducer;
    private final CoreDelegationService coreDelegationService;
    private final CoreRideStatusService coreRideStatusService;

    public RideService(
            RideRepository rideRepository,
            DriverRepository driverRepository,
            PassengerRepository passengerRepository,
            PendingRidePool pendingRidePool,
            LoggingService loggingService,
            OverflowPolicyService overflowPolicyService,
            RideProducer rideProducer,
            CoreDelegationService coreDelegationService,
            CoreRideStatusService coreRideStatusService
    ) {
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
        this.passengerRepository = passengerRepository;
        this.pendingRidePool = pendingRidePool;
        this.loggingService = loggingService;
        this.overflowPolicyService = overflowPolicyService;
        this.rideProducer = rideProducer;
        this.coreDelegationService = coreDelegationService;
        this.coreRideStatusService = coreRideStatusService;
    }

    public Ride createRide(Long passengerId, Ride ride) {

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));

        ride.setPassenger(passenger);
        ride.setStatus(RideStatus.REQUESTED);
        ride.setCreatedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        loggingService.info(
                new LogEvent(
                        LocalDateTime.now().toString(),
                        "RIDE_CREATED",
                        savedRide.getId(),
                        "PediUber",
                        null,
                        RideStatus.REQUESTED.name(),
                        null
                )
        );

        if (overflowPolicyService.isOverloaded()) {

            try {

                RideAccepted rideAccepted =
                        coreDelegationService.delegateRide(savedRide);

                savedRide.setCoreRideUuid(rideAccepted.getRideUuid());
                savedRide.setOriginServiceId("pediuber");
                savedRide.setLogicalTimestamp(rideAccepted.getLogicalTimestamp());

                Ride delegatedRide = rideRepository.save(savedRide);

                loggingService.warn(
                        new LogEvent(
                                LocalDateTime.now().toString(),
                                "RIDE_DELEGATED_TO_CORE",
                                delegatedRide.getId(),
                                "PediUber",
                                delegatedRide.getStatus().name(),
                                delegatedRide.getStatus().name(),
                                null
                        )
                );

                return delegatedRide;

            } catch (RestClientException exception) {

                loggingService.error(
                        new LogEvent(
                                LocalDateTime.now().toString(),
                                "CORE_DELEGATION_FAILED",
                                savedRide.getId(),
                                "PediUber",
                                savedRide.getStatus().name(),
                                savedRide.getStatus().name(),
                                null
                        )
                );

                throw new RuntimeException("Failed to delegate ride to Core");
            }
        }

        pendingRidePool.addRide(savedRide);

        return savedRide;
    }

    public Ride updateRideStatus(Long rideId, RideStatus newStatus) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        RideStatus previousStatus = ride.getStatus();

        if (previousStatus == newStatus) {
            return ride;
        }

        validateStatusTransition(previousStatus, newStatus);

        try {

            coreRideStatusService.notifyStatusChange(
                    ride,
                    newStatus
            );

        } catch (RestClientResponseException exception) {

            System.out.println("[CORE_STATUS_UPDATE_FAILED]");
            System.out.println("HTTP Status: " + exception.getStatusCode());
            System.out.println("Response Body: " + exception.getResponseBodyAsString());

            loggingService.warn(
                    new LogEvent(
                            LocalDateTime.now().toString(),
                            "CORE_STATUS_UPDATE_FAILED",
                            ride.getId(),
                            "PediUber",
                            previousStatus.name(),
                            newStatus.name(),
                            exception.getResponseBodyAsString()
                    )
            );

            throw new RuntimeException(
                    "Failed to notify Core about ride status: "
                            + exception.getStatusCode()
                            + " - "
                            + exception.getResponseBodyAsString()
            );

        } catch (RestClientException exception) {

            System.out.println("[CORE_STATUS_UPDATE_FAILED]");
            System.out.println("Error: " + exception.getMessage());

            loggingService.warn(
                    new LogEvent(
                            LocalDateTime.now().toString(),
                            "CORE_STATUS_UPDATE_FAILED",
                            ride.getId(),
                            "PediUber",
                            previousStatus.name(),
                            newStatus.name(),
                            exception.getMessage()
                    )
            );

            throw new RuntimeException(
                    "Failed to notify Core about ride status: " + exception.getMessage()
            );
        }

        ride.setStatus(newStatus);

        if (shouldReleaseDriver(newStatus)) {
            releaseDriverIfNecessary(ride);
        }

        Ride savedRide = rideRepository.save(ride);

        loggingService.info(
                new LogEvent(
                        LocalDateTime.now().toString(),
                        "RIDE_STATUS_UPDATED",
                        savedRide.getId(),
                        "PediUber",
                        previousStatus.name(),
                        newStatus.name(),
                        null
                )
        );

        return savedRide;
    }

    public Ride confirmRide(Long rideId) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (ride.getDriver() == null) {
            throw new RuntimeException("Ride has no assigned driver");
        }

        return updateRideStatus(rideId, RideStatus.CONFIRMED);
    }

    public Ride startRide(Long rideId) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (ride.getDriver() == null) {
            throw new RuntimeException("Ride has no assigned driver");
        }

        return updateRideStatus(rideId, RideStatus.IN_TRANSIT);
    }

    public Ride completeRide(Long rideId) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (ride.getDriver() == null) {
            throw new RuntimeException("Ride has no assigned driver");
        }

        return updateRideStatus(rideId, RideStatus.COMPLETED);
    }

    public Ride cancelRide(Long rideId) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (shouldSendCompensationToCore(ride)) {
            return updateRideStatus(rideId, RideStatus.COMPENSATING);
        }

        return updateRideStatus(rideId, RideStatus.CANCELLED);
    }

    public Ride matchDriver(Long rideId, Long driverId) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> {

                    loggingService.error(
                            new LogEvent(
                                    LocalDateTime.now().toString(),
                                    "RIDE_NOT_FOUND",
                                    rideId,
                                    "PediUber",
                                    null,
                                    null,
                                    null
                            )
                    );

                    return new RuntimeException("Ride not found");
                });

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> {

                    loggingService.error(
                            new LogEvent(
                                    LocalDateTime.now().toString(),
                                    "DRIVER_NOT_FOUND",
                                    rideId,
                                    "PediUber",
                                    null,
                                    null,
                                    null
                            )
                    );

                    return new RuntimeException("Driver not found");
                });

        if (!driver.getAvailable()) {

            loggingService.warn(
                    new LogEvent(
                            LocalDateTime.now().toString(),
                            "DRIVER_UNAVAILABLE",
                            ride.getId(),
                            "PediUber",
                            ride.getStatus().name(),
                            ride.getStatus().name(),
                            null
                    )
            );

            throw new RuntimeException("Driver unavailable");
        }

        ride.setDriver(driver);

        ride.setStatus(RideStatus.MATCHED);

        driver.setAvailable(false);

        loggingService.info(
                new LogEvent(
                        LocalDateTime.now().toString(),
                        "RIDE_MATCHED",
                        ride.getId(),
                        "PediUber",
                        RideStatus.REQUESTED.name(),
                        RideStatus.MATCHED.name(),
                        null
                )
        );

        return rideRepository.save(ride);
    }

    private boolean shouldReleaseDriver(RideStatus status) {

        return status == RideStatus.COMPLETED
                || status == RideStatus.CANCELLED
                || status == RideStatus.COMPENSATING;
    }

    private void releaseDriverIfNecessary(Ride ride) {

        Driver driver = ride.getDriver();

        if (driver == null) {
            return;
        }

        driver.setAvailable(true);
        driverRepository.save(driver);
    }

    private boolean shouldSendCompensationToCore(Ride ride) {

        return ride.getCoreRideUuid() != null
                && !ride.getCoreRideUuid().isBlank()
                && ride.getDriver() != null
                && ride.getStatus() != RideStatus.COMPLETED
                && ride.getStatus() != RideStatus.CANCELLED
                && ride.getStatus() != RideStatus.COMPENSATING;
    }

    private void validateStatusTransition(
            RideStatus currentStatus,
            RideStatus newStatus
    ) {

        if (currentStatus == newStatus) {
            return;
        }

        switch (currentStatus) {

            case REQUESTED -> {
                if (newStatus != RideStatus.MATCHED &&
                        newStatus != RideStatus.CANCELLED &&
                        newStatus != RideStatus.COMPENSATING) {

                    throw new RuntimeException(
                            "Invalid transition from REQUESTED to " + newStatus
                    );
                }
            }

            case MATCHED -> {
                if (newStatus != RideStatus.CONFIRMED &&
                        newStatus != RideStatus.CANCELLED &&
                        newStatus != RideStatus.COMPENSATING) {

                    throw new RuntimeException(
                            "Invalid transition from MATCHED to " + newStatus
                    );
                }
            }

            case CONFIRMED -> {
                if (newStatus != RideStatus.IN_TRANSIT &&
                        newStatus != RideStatus.CANCELLED &&
                        newStatus != RideStatus.COMPENSATING) {

                    throw new RuntimeException(
                            "Invalid transition from CONFIRMED to " + newStatus
                    );
                }
            }

            case IN_TRANSIT -> {
                if (newStatus != RideStatus.COMPLETED &&
                        newStatus != RideStatus.COMPENSATING) {

                    throw new RuntimeException(
                            "Invalid transition from IN_TRANSIT to " + newStatus
                    );
                }
            }

            case COMPENSATING -> {
                if (newStatus != RideStatus.CANCELLED) {

                    throw new RuntimeException(
                            "Invalid transition from COMPENSATING to " + newStatus
                    );
                }
            }

            case COMPLETED, CANCELLED -> {
                throw new RuntimeException(
                        "Ride already finalized"
                );
            }
        }
    }
}
