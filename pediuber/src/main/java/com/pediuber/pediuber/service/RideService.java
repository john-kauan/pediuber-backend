package com.pediuber.pediuber.service;


import com.pediuber.pediuber.dto.CreateRideRequest;
import com.pediuber.pediuber.entity.Driver;
import com.pediuber.pediuber.entity.Passenger;
import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.enums.RideStatus;
import com.pediuber.pediuber.repository.DriverRepository;
import com.pediuber.pediuber.repository.PassengerRepository;
import com.pediuber.pediuber.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final PassengerRepository passengerRepository;

    public RideService(
            RideRepository rideRepository,
            DriverRepository driverRepository,
            PassengerRepository passengerRepository
    ) {
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
        this.passengerRepository = passengerRepository;
    }

    public Ride createRide(Long passengerId, Ride ride) {

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));

        ride.setPassenger(passenger);
        ride.setStatus(RideStatus.REQUESTED);
        ride.setCreatedAt(LocalDateTime.now());

        return rideRepository.save(ride);
    }

    public Ride updateRideStatus(Long rideId, RideStatus newStatus) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        validateStatusTransition(ride.getStatus(), newStatus);

        ride.setStatus(newStatus);

        return rideRepository.save(ride);
    }

    public Ride matchDriver(Long rideId, Long driverId) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        if (!driver.getAvailable()) {
            throw new RuntimeException("Driver unavailable");
        }

        ride.setDriver(driver);

        ride.setStatus(RideStatus.MATCHED);

        driver.setAvailable(false);

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
