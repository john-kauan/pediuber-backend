package com.pediuber.pediuber.policy;


import com.pediuber.pediuber.pool.PendingRidePool;
import com.pediuber.pediuber.repository.DriverRepository;
import org.springframework.stereotype.Service;

@Service
public class OverflowPolicyService {

    private static final int MAX_PENDING_RIDES = 5;

    private final DriverRepository driverRepository;
    private final PendingRidePool pendingRidePool;

    public OverflowPolicyService(DriverRepository driverRepository,
                                 PendingRidePool pendingRidePool) {

        this.driverRepository = driverRepository;
        this.pendingRidePool = pendingRidePool;
    }

    public boolean isOverloaded() {

        boolean noAvailableDrivers =
                !driverRepository.existsByAvailableTrue();

        boolean queueFull =
                pendingRidePool.size() >= MAX_PENDING_RIDES;

        return noAvailableDrivers || queueFull;
    }

}
