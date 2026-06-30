package com.pediuber.pediuber.service;

import com.pediuber.pediuber.dto.DriverCurrentRideResponse;
import com.pediuber.pediuber.entity.Driver;
import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.enums.RideStatus;
import com.pediuber.pediuber.repository.DriverRepository;
import com.pediuber.pediuber.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;

    public DriverService(
            DriverRepository driverRepository,
            RideRepository rideRepository
    ) {
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
    }

    public Driver createDriver(Driver driver) {

        if (driver.getAvailable() == null) {
            driver.setAvailable(true);
        }

        return driverRepository.save(driver);
    }

    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    public Driver getDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
    }

    public Driver updateAvailability(Long id, Boolean available) {

        if (available == null) {
            throw new RuntimeException("Availability cannot be null");
        }

        Driver driver = getDriverById(id);

        driver.setAvailable(available);

        return driverRepository.save(driver);
    }

    public DriverCurrentRideResponse getCurrentRide(Long driverId) {

        Driver driver = getDriverById(driverId);

        List<RideStatus> activeStatuses = List.of(
                RideStatus.MATCHED,
                RideStatus.CONFIRMED,
                RideStatus.IN_TRANSIT
        );

        Optional<Ride> currentRide =
                rideRepository.findFirstByDriverIdAndStatusInOrderByCreatedAtDesc(
                        driverId,
                        activeStatuses
                );

        return currentRide
                .map(ride -> DriverCurrentRideResponse.fromRide(driver, ride))
                .orElseGet(() -> DriverCurrentRideResponse.noRide(driver));
    }
}