package com.pediuber.pediuber.controller;

import com.pediuber.pediuber.dto.DriverAvailabilityRequest;
import com.pediuber.pediuber.entity.Driver;
import com.pediuber.pediuber.service.DriverService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    public Driver createDriver(@RequestBody Driver driver) {
        return driverService.createDriver(driver);
    }

    @GetMapping
    public List<Driver> getAllDrivers() {
        return driverService.getAllDrivers();
    }

    @GetMapping("/{id}")
    public Driver getDriverById(@PathVariable Long id) {
        return driverService.getDriverById(id);
    }

    @PatchMapping("/{id}/availability")
    public Driver updateAvailability(
            @PathVariable Long id,
            @RequestBody DriverAvailabilityRequest request
    ) {
        return driverService.updateAvailability(
                id,
                request.available()
        );
    }
}