package com.pediuber.pediuber.service;


import com.pediuber.pediuber.dto.CreateRideRequest;
import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.enums.RideStatus;
import com.pediuber.pediuber.repository.RideRepository;
import org.springframework.stereotype.Service;

@Service
public class RideService {

    private final RideRepository rideRepository;

    public RideService(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    public Ride createRide(CreateRideRequest request) {

        Ride ride = new Ride(
                request.getOrigin(),
                request.getDestination(),
                request.getPassengerId()
        );

        return rideRepository.save(ride);
    }

    public Ride updateRideStatus(Long rideId, RideStatus newStatus) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        validateStatusTransition(ride.getStatus(), newStatus);

        ride.setStatus(newStatus);

        return rideRepository.save(ride);
    }

    private void validateStatusTransition(

            RideStatus currentStatus,
            RideStatus newStatus


    ) {

        System.out.println("CURRENT: " + currentStatus);
        System.out.println("NEW: " + newStatus);

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
