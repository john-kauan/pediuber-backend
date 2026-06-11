package com.pediuber.pediuber.health;

import com.pediuber.pediuber.dto.HealthResponse;
import com.pediuber.pediuber.logging.LogEvent;
import com.pediuber.pediuber.logging.LoggingService;
import com.pediuber.pediuber.pool.PendingRidePool;
import com.pediuber.pediuber.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class HealthService {

    private final DriverRepository driverRepository;
    private final PendingRidePool pendingRidePool;
    private final LoggingService loggingService;

    public HealthService(
            DriverRepository driverRepository,
            PendingRidePool pendingRidePool,
            LoggingService loggingService
    ) {
        this.driverRepository = driverRepository;
        this.pendingRidePool = pendingRidePool;
        this.loggingService = loggingService;
    }

    public HealthResponse getHealth() {

        long availableDrivers = driverRepository.countByAvailableTrue();

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

        int pendingRides = pendingRidePool.size();

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

        String status;

        if (availableDrivers == 0 || pendingRides > 10) {
            status = "DEGRADED";
        }
        else {
            status = "UP";
        }

        double averageLatency = 50.0;

        return new HealthResponse(
                status,
                availableDrivers,
                pendingRides,
                averageLatency
        );
    }

}
