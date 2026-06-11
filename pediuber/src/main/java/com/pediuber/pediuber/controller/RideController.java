package com.pediuber.pediuber.controller;

import com.pediuber.pediuber.dto.CreateRideRequest;
import com.pediuber.pediuber.dto.RideQueueMessage;
import com.pediuber.pediuber.dto.UpdateRideStatusRequest;
import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.pool.PendingRidePool;
import com.pediuber.pediuber.rabbitmq.RideProducer;
import com.pediuber.pediuber.service.RideService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rides")
public class RideController {

        private final RideService rideService;
        private final PendingRidePool pendingRidePool;
        private final RideProducer rideProducer;

        public RideController(RideService rideService, PendingRidePool pendingRidePool, RideProducer rideProducer) {

            this.rideService = rideService;
            this.pendingRidePool = pendingRidePool;
            this.rideProducer = rideProducer;
        }

        @PostMapping("/passenger/{passengerId}")
        public Ride createRide(@PathVariable Long passengerId,
                           @RequestBody Ride ride) {

        return rideService.createRide(passengerId, ride);
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

        @GetMapping("/pool/size")
        public int getPoolSize() {
            return pendingRidePool.size();
        }


}
