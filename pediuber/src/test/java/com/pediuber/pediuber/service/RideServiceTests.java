package com.pediuber.pediuber.service;

import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.enums.RideStatus;
import com.pediuber.pediuber.pool.PendingRidePool;
import com.pediuber.pediuber.repository.DriverRepository;
import com.pediuber.pediuber.repository.PassengerRepository;
import com.pediuber.pediuber.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RideServiceTests {

    private RideRepository rideRepository;
    private DriverRepository driverRepository;
    private PassengerRepository passengerRepository;
    private PendingRidePool pendingRidePool;

    private RideService rideService;

    @BeforeEach
    void setup() {

        rideRepository = Mockito.mock(RideRepository.class);

        driverRepository = Mockito.mock(DriverRepository.class);

        passengerRepository = Mockito.mock(PassengerRepository.class);

        pendingRidePool = Mockito.mock(PendingRidePool.class);

        rideService = new RideService(
                rideRepository,
                driverRepository,
                passengerRepository,
                pendingRidePool
        );
    }

    @Test
    void shouldUpdateRideStatusFromRequestedToMatched() {

        Ride ride = new Ride();

        ride.setStatus(RideStatus.REQUESTED);

        Mockito.when(
                rideRepository.findById(1L)
        ).thenReturn(java.util.Optional.of(ride));

        Mockito.when(
                rideRepository.save(Mockito.any())
        ).thenAnswer(i -> i.getArgument(0));

        Ride result =
                rideService.updateRideStatus(
                        1L,
                        RideStatus.MATCHED
                );

        assert result.getStatus() == RideStatus.MATCHED;
    }

    @Test
    void shouldThrowExceptionForInvalidTransition() {

        Ride ride = new Ride();

        ride.setStatus(RideStatus.REQUESTED);

        Mockito.when(
                rideRepository.findById(1L)
        ).thenReturn(java.util.Optional.of(ride));

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> rideService.updateRideStatus(
                        1L,
                        RideStatus.COMPLETED
                )
        );
    }

}
