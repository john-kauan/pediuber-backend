package com.pediuber.pediuber.repository;

import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByStatus(RideStatus status);
    Optional<Ride> findByCoreRideUuid(String coreRideUuid);

    Optional<Ride> findFirstByDriverIdAndStatusInOrderByCreatedAtDesc(
            Long driverId,
            List<RideStatus> statuses
    );

}
