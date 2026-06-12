package com.pediuber.pediuber.service;

import com.pediuber.pediuber.core.dto.RideAccepted;
import com.pediuber.pediuber.entity.Passenger;
import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.enums.RideStatus;
import com.pediuber.pediuber.logging.LoggingService;
import com.pediuber.pediuber.policy.OverflowPolicyService;
import com.pediuber.pediuber.pool.PendingRidePool;
import com.pediuber.pediuber.rabbitmq.RideProducer;
import com.pediuber.pediuber.repository.DriverRepository;
import com.pediuber.pediuber.repository.PassengerRepository;
import com.pediuber.pediuber.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.pediuber.pediuber.core.service.CoreDelegationService;

import java.time.LocalDateTime;
import java.util.Optional;
import com.pediuber.pediuber.core.service.CoreRideStatusService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RideServiceTests {

    private RideRepository rideRepository;
    private DriverRepository driverRepository;
    private PassengerRepository passengerRepository;
    private PendingRidePool pendingRidePool;
    private LoggingService loggingService;
    private RideService rideService;
    private OverflowPolicyService overflowPolicyService;
    private RideProducer rideProducer;
    private CoreDelegationService coreDelegationService;
    private CoreRideStatusService coreRideStatusService;

    @BeforeEach
    void setup() {

        rideRepository = Mockito.mock(RideRepository.class);

        driverRepository = Mockito.mock(DriverRepository.class);

        passengerRepository = Mockito.mock(PassengerRepository.class);

        pendingRidePool = Mockito.mock(PendingRidePool.class);

        loggingService = Mockito.mock(LoggingService.class);

        overflowPolicyService = Mockito.mock(OverflowPolicyService.class);

        rideProducer = Mockito.mock(RideProducer.class);

        coreDelegationService = Mockito.mock(CoreDelegationService.class);

        coreRideStatusService = Mockito.mock(CoreRideStatusService.class);

        rideService = new RideService(
                rideRepository,
                driverRepository,
                passengerRepository,
                pendingRidePool,
                loggingService,
                overflowPolicyService,
                rideProducer,
                coreDelegationService,
                coreRideStatusService

        );
    }

    @Test
    void shouldUpdateRideStatusFromRequestedToMatched() {

        Ride ride = new Ride();
        ride.setStatus(RideStatus.REQUESTED);

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        when(rideRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        Ride result = rideService.updateRideStatus(
                1L,
                RideStatus.MATCHED
        );

        assertEquals(RideStatus.MATCHED, result.getStatus());
    }

    @Test
    void shouldThrowExceptionForInvalidTransition() {

        Ride ride = new Ride();

        ride.setStatus(RideStatus.REQUESTED);

        when(
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

    @Test
    void shouldCreateRideAndAddToPendingPoolWhenNotOverloaded() {

        Long passengerId = 1L;

        Passenger passenger = new Passenger();
        passenger.setId(passengerId);

        Ride ride = new Ride();
        ride.setOrigin("UFV");
        ride.setDestination("Centro");

        Ride savedRide = new Ride();
        savedRide.setId(10L);
        savedRide.setOrigin("UFV");
        savedRide.setDestination("Centro");
        savedRide.setPassenger(passenger);
        savedRide.setStatus(RideStatus.REQUESTED);
        savedRide.setCreatedAt(LocalDateTime.now());

        when(passengerRepository.findById(passengerId))
                .thenReturn(Optional.of(passenger));

        when(rideRepository.save(any(Ride.class)))
                .thenReturn(savedRide);

        when(overflowPolicyService.isOverloaded())
                .thenReturn(false);

        Ride result = rideService.createRide(passengerId, ride);

        assertEquals(RideStatus.REQUESTED, result.getStatus());
        assertEquals(passenger, result.getPassenger());

        verify(rideRepository).save(any(Ride.class));
        verify(pendingRidePool).addRide(savedRide);
        verify(rideProducer, never()).sendRideToOutputQueue(any());
    }

    @Test
    void shouldDelegateRideToCoreWhenOverloaded() {

        Long passengerId = 1L;

        Passenger passenger = new Passenger();
        passenger.setId(passengerId);

        Ride ride = new Ride();
        ride.setOrigin("UFV");
        ride.setDestination("Centro");

        Ride savedRide = new Ride();
        savedRide.setId(10L);
        savedRide.setOrigin("UFV");
        savedRide.setDestination("Centro");
        savedRide.setPassenger(passenger);
        savedRide.setStatus(RideStatus.REQUESTED);
        savedRide.setCreatedAt(LocalDateTime.now());

        RideAccepted rideAccepted = new RideAccepted(
                "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                10L,
                "Corrida aceita pelo Core"
        );

        when(passengerRepository.findById(passengerId))
                .thenReturn(Optional.of(passenger));

        when(rideRepository.save(any(Ride.class)))
                .thenReturn(savedRide);

        when(overflowPolicyService.isOverloaded())
                .thenReturn(true);

        when(coreDelegationService.delegateRide(savedRide))
                .thenReturn(rideAccepted);

        Ride result = rideService.createRide(passengerId, ride);

        assertEquals(RideStatus.REQUESTED, result.getStatus());
        assertEquals("f47ac10b-58cc-4372-a567-0e02b2c3d479", result.getCoreRideUuid());
        assertEquals(10L, result.getLogicalTimestamp());

        verify(rideRepository, times(2)).save(any(Ride.class));

        verify(coreDelegationService).delegateRide(savedRide);

        verify(rideProducer, never()).sendRideToOutputQueue(any());

        verify(pendingRidePool, never()).addRide(any(Ride.class));
    }

}
