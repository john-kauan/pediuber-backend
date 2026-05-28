package com.pediuber.pediuber.controller;

import com.pediuber.pediuber.dto.CreateRideRequest;
import com.pediuber.pediuber.dto.UpdateRideStatusRequest;
import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.service.RideService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rides")
public class RideController {

        private final RideService rideService;

        public RideController(RideService rideService) {
            this.rideService = rideService;
        }

        @PostMapping
        public Ride createRide(
                @RequestBody CreateRideRequest request
        ) {
            return rideService.createRide(request);
        }

        @PatchMapping("/{rideId}/status")
        public Ride updateRideStatus(
                @PathVariable Long rideId,
                @RequestBody UpdateRideStatusRequest request
        ) {
            return rideService.updateRideStatus(
                    rideId,
                    request.getStatus()
            );
        }

        @PatchMapping("/{rideId}/match/{driverId}")
         public Ride matchDriver(
            @PathVariable Long rideId,
            @PathVariable Long driverId
         ) {
        return rideService.matchDriver(rideId, driverId);
        }

}
