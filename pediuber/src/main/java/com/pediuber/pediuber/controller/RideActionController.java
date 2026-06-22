package com.pediuber.pediuber.controller;

import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.service.RideService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rides")
public class RideActionController {

    private final RideService rideService;

    public RideActionController(RideService rideService) {
        this.rideService = rideService;
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<Ride> startRide(@PathVariable Long id) {

        Ride ride = rideService.startRide(id);

        return ResponseEntity.ok(ride);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Ride> completeRide(@PathVariable Long id) {

        Ride ride = rideService.completeRide(id);

        return ResponseEntity.ok(ride);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Ride> cancelRide(@PathVariable Long id) {

        Ride ride = rideService.cancelRide(id);

        return ResponseEntity.ok(ride);
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Ride> confirmRide(@PathVariable Long id) {

        Ride ride = rideService.confirmRide(id);

        return ResponseEntity.ok(ride);
    }
}