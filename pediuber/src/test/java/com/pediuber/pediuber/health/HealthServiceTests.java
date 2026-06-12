package com.pediuber.pediuber.health;

import com.pediuber.pediuber.dto.HealthResponse;
import com.pediuber.pediuber.logging.LogEvent;
import com.pediuber.pediuber.logging.LoggingService;
import com.pediuber.pediuber.pool.PendingRidePool;
import com.pediuber.pediuber.rabbitmq.RabbitMQHealthService;
import com.pediuber.pediuber.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class HealthServiceTests {

    private DriverRepository driverRepository;
    private PendingRidePool pendingRidePool;
    private LoggingService loggingService;
    private RabbitMQHealthService rabbitMQHealthService;
    private HealthService healthService;

    @BeforeEach
    void setup() {

        driverRepository = Mockito.mock(DriverRepository.class);
        pendingRidePool = Mockito.mock(PendingRidePool.class);
        loggingService = Mockito.mock(LoggingService.class);
        rabbitMQHealthService = Mockito.mock(RabbitMQHealthService.class);

        healthService = new HealthService(
                driverRepository,
                pendingRidePool,
                loggingService,
                rabbitMQHealthService
        );
    }

    @Test
    void shouldReturnUpWhenServiceIsHealthy() {

        when(driverRepository.countByAvailableTrue()).thenReturn(2L);

        when(pendingRidePool.size()).thenReturn(0);

        mockHealthyRabbitQueues();

        HealthResponse response = healthService.getHealth();

        assertEquals("UP", response.getStatus());
        assertEquals(2L, response.getAvailableDrivers());
        assertEquals(0, response.getPendingRides());
        assertEquals(50.0, response.getAverageLatency());
        assertEquals(0, response.getInputQueueMessages());
        assertEquals(0, response.getOutputQueueMessages());
        assertEquals(0, response.getInputDlqMessages());
        assertEquals(0, response.getOutputDlqMessages());

        verify(loggingService, never()).warn(any(LogEvent.class));
    }

    @Test
    void shouldReturnDegradedWhenThereAreNoAvailableDrivers() {

        when(driverRepository.countByAvailableTrue()).thenReturn(0L);

        when(pendingRidePool.size()).thenReturn(0);

        mockHealthyRabbitQueues();

        HealthResponse response = healthService.getHealth();

        assertEquals("DEGRADED", response.getStatus());
        assertEquals(0L, response.getAvailableDrivers());

        verify(loggingService).warn(any(LogEvent.class));
    }

    @Test
    void shouldReturnDegradedWhenPendingRidePoolIsTooLarge() {

        when(driverRepository.countByAvailableTrue()).thenReturn(2L);

        when(pendingRidePool.size()).thenReturn(11);

        mockHealthyRabbitQueues();

        HealthResponse response = healthService.getHealth();

        assertEquals("DEGRADED", response.getStatus());
        assertEquals(11, response.getPendingRides());

        verify(loggingService).warn(any(LogEvent.class));
    }

    @Test
    void shouldReturnDegradedWhenThereAreMessagesInDlq() {

        when(driverRepository.countByAvailableTrue()).thenReturn(2L);

        when(pendingRidePool.size()).thenReturn(0);

        when(rabbitMQHealthService.getInputQueueMessages()).thenReturn(0);

        when(rabbitMQHealthService.getOutputQueueMessages()).thenReturn(0);

        when(rabbitMQHealthService.getInputDlqMessages()).thenReturn(1);

        when(rabbitMQHealthService.getOutputDlqMessages()).thenReturn(0);

        HealthResponse response = healthService.getHealth();

        assertEquals("DEGRADED", response.getStatus());
        assertEquals(1, response.getInputDlqMessages());

        verify(loggingService).warn(any(LogEvent.class));
    }

    @Test
    void shouldReturnDegradedWhenRabbitQueueIsUnavailable() {

        when(driverRepository.countByAvailableTrue()).thenReturn(2L);

        when(pendingRidePool.size()).thenReturn(0);

        when(rabbitMQHealthService.getInputQueueMessages()).thenReturn(-1);

        when(rabbitMQHealthService.getOutputQueueMessages()).thenReturn(0);

        when(rabbitMQHealthService.getInputDlqMessages()).thenReturn(0);

        when(rabbitMQHealthService.getOutputDlqMessages()).thenReturn(0);

        HealthResponse response = healthService.getHealth();

        assertEquals("DEGRADED", response.getStatus());
        assertEquals(-1, response.getInputQueueMessages());
    }

    private void mockHealthyRabbitQueues() {

        when(rabbitMQHealthService.getInputQueueMessages()).thenReturn(0);

        when(rabbitMQHealthService.getOutputQueueMessages()).thenReturn(0);

        when(rabbitMQHealthService.getInputDlqMessages()).thenReturn(0);

        when(rabbitMQHealthService.getOutputDlqMessages()).thenReturn(0);
    }
}