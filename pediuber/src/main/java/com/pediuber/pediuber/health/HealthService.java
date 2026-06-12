package com.pediuber.pediuber.health;

import com.pediuber.pediuber.dto.HealthResponse;
import com.pediuber.pediuber.logging.LogEvent;
import com.pediuber.pediuber.logging.LoggingService;
import com.pediuber.pediuber.pool.PendingRidePool;
import com.pediuber.pediuber.rabbitmq.RabbitMQHealthService;
import com.pediuber.pediuber.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class HealthService {

    private final DriverRepository driverRepository;
    private final PendingRidePool pendingRidePool;
    private final LoggingService loggingService;
    private final RabbitMQHealthService rabbitMQHealthService;

    public HealthService(
            DriverRepository driverRepository,
            PendingRidePool pendingRidePool,
            LoggingService loggingService,
            RabbitMQHealthService rabbitMQHealthService
    ) {
        this.driverRepository = driverRepository;
        this.pendingRidePool = pendingRidePool;
        this.loggingService = loggingService;
        this.rabbitMQHealthService = rabbitMQHealthService;
    }

    public HealthResponse getHealth() {

        long availableDrivers = driverRepository.countByAvailableTrue();

        int pendingRides = pendingRidePool.size();

        double averageLatency = 50.0;

        int inputQueueMessages = rabbitMQHealthService.getInputQueueMessages();

        int outputQueueMessages = rabbitMQHealthService.getOutputQueueMessages();

        int inputDlqMessages = rabbitMQHealthService.getInputDlqMessages();

        int outputDlqMessages = rabbitMQHealthService.getOutputDlqMessages();

        String status = "UP";

        if (availableDrivers == 0 || pendingRides > 10) {
            status = "DEGRADED";
        }

        if (inputQueueMessages < 0 ||
                outputQueueMessages < 0 ||
                inputDlqMessages < 0 ||
                outputDlqMessages < 0) {

            status = "DEGRADED";
        }

        if (inputDlqMessages > 0 || outputDlqMessages > 0) {
            status = "DEGRADED";
        }

        if (pendingRides > 10) {
            loggingService.warn(
                    new LogEvent(
                            LocalDateTime.now().toString(),
                            "HIGH_QUEUE_SIZE",
                            null,
                            "PediUber",
                            null,
                            null,
                            null
                    )
            );
        }

        if (availableDrivers == 0) {
            loggingService.warn(
                    new LogEvent(
                            LocalDateTime.now().toString(),
                            "NO_AVAILABLE_DRIVERS",
                            null,
                            "PediUber",
                            null,
                            null,
                            null
                    )
            );
        }

        if (inputDlqMessages > 0 || outputDlqMessages > 0) {
            loggingService.warn(
                    new LogEvent(
                            LocalDateTime.now().toString(),
                            "DLQ_MESSAGES_FOUND",
                            null,
                            "PediUber",
                            null,
                            null,
                            null
                    )
            );
        }

        return new HealthResponse(
                status,
                availableDrivers,
                pendingRides,
                averageLatency,
                inputQueueMessages,
                outputQueueMessages,
                inputDlqMessages,
                outputDlqMessages
        );
    }
}