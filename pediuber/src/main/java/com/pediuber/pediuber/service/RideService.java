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
import com.pediuber.pediuber.dto.RideQueueMessage;
import com.pediuber.pediuber.policy.OverflowPolicyService;
import com.pediuber.pediuber.rabbitmq.RideProducer;

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

    public RideService(
            RideRepository rideRepository,
            DriverRepository driverRepository,
            PassengerRepository passengerRepository,
            PendingRidePool pendingRidePool,
            LoggingService loggingService,
            OverflowPolicyService overflowPolicyService,
            RideProducer rideProducer
    ) {
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
        this.passengerRepository = passengerRepository;
        this.pendingRidePool = pendingRidePool;
        this.loggingService = loggingService;
        this.overflowPolicyService = overflowPolicyService;
        this.rideProducer = rideProducer;
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

            RideQueueMessage message = new RideQueueMessage(
                    savedRide.getId(),
                    savedRide.getOrigin(),
                    savedRide.getDestination(),
                    passenger.getId(),
                    savedRide.getStatus().name(),
                    LocalDateTime.now().toString(),
                    0
            );

            rideProducer.sendRideToOutputQueue(message);

            loggingService.warn(
                    new LogEvent(
                            LocalDateTime.now().toString(),
                            "RIDE_SENT_TO_OUTPUT_QUEUE",
                            savedRide.getId(),
                            "PediUber",
                            savedRide.getStatus().name(),
                            savedRide.getStatus().name(),
                            null
                    )
            );

            return savedRide;
        }

        pendingRidePool.addRide(savedRide);

        return savedRide;
    }

    public Ride updateRideStatus(Long rideId, RideStatus newStatus) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        validateStatusTransition(ride.getStatus(), newStatus);

        RideStatus previousStatus = ride.getStatus();

        ride.setStatus(newStatus);

        loggingService.info(
                new LogEvent(
                        LocalDateTime.now().toString(),
                        "RIDE_STATUS_UPDATED",
                        ride.getId(),
                        "PediUber",
                        previousStatus.name(),
                        newStatus.name(),
                        null
                )
        );

        return rideRepository.save(ride);
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

    private void validateStatusTransition(
            RideStatus currentStatus,
            RideStatus newStatus
    ) {

        switch (currentStatus) {

            case REQUESTED -> {
                if (newStatus != RideStatus.MATCHED &&
                        newStatus != RideStatus.CANCELLED) {

                    throw new RuntimeException(
                            "Invalid transition from REQUESTED to " + newStatus
                    );
                }
            }

            case MATCHED -> {
                if (newStatus != RideStatus.CONFIRMED &&
                        newStatus != RideStatus.CANCELLED) {

                    throw new RuntimeException(
                            "Invalid transition from MATCHED to " + newStatus
                    );
                }
            }

            case CONFIRMED -> {
                if (newStatus != RideStatus.IN_TRANSIT &&
                        newStatus != RideStatus.CANCELLED) {

                    throw new RuntimeException(
                            "Invalid transition from CONFIRMED to " + newStatus
                    );
                }
            }

            case IN_TRANSIT -> {
                if (newStatus != RideStatus.COMPLETED) {

                    throw new RuntimeException(
                            "Invalid transition from IN_TRANSIT to " + newStatus
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
