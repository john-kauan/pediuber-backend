package com.pediuber.pediuber.rabbitmq;

import com.pediuber.pediuber.dto.RideQueueMessage;
import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.enums.RideStatus;
import com.pediuber.pediuber.logging.LogEvent;
import com.pediuber.pediuber.logging.LoggingService;
import com.pediuber.pediuber.pool.PendingRidePool;
import com.pediuber.pediuber.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RideConsumerTests {

    private RideRepository rideRepository;
    private PendingRidePool pendingRidePool;
    private LoggingService loggingService;
    private RideConsumer rideConsumer;

    @BeforeEach
    void setup() {

        rideRepository = Mockito.mock(RideRepository.class);
        pendingRidePool = Mockito.mock(PendingRidePool.class);
        loggingService = Mockito.mock(LoggingService.class);

        rideConsumer = new RideConsumer(
                rideRepository,
                pendingRidePool,
                loggingService
        );
    }

    @Test
    void shouldConsumeRideMessageAndAddRideToPendingPool() {

        RideQueueMessage message = new RideQueueMessage(
                999L,
                "UFV",
                "Centro",
                1L,
                "REQUESTED",
                LocalDateTime.now().toString(),
                0
        );

        Ride savedRide = new Ride();
        savedRide.setId(10L);
        savedRide.setOrigin("UFV");
        savedRide.setDestination("Centro");
        savedRide.setStatus(RideStatus.REQUESTED);
        savedRide.setCreatedAt(LocalDateTime.now());

        when(rideRepository.save(any(Ride.class)))
                .thenReturn(savedRide);

        rideConsumer.consumeRide(message);

        ArgumentCaptor<Ride> rideCaptor =
                ArgumentCaptor.forClass(Ride.class);

        verify(rideRepository).save(rideCaptor.capture());

        Ride rideToSave = rideCaptor.getValue();

        assertEquals("UFV", rideToSave.getOrigin());
        assertEquals("Centro", rideToSave.getDestination());
        assertEquals(RideStatus.REQUESTED, rideToSave.getStatus());
        assertNotNull(rideToSave.getCreatedAt());

        verify(pendingRidePool).addRide(savedRide);
        verify(loggingService).info(any(LogEvent.class));
    }
}