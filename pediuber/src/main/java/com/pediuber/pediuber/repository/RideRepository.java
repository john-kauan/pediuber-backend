package com.pediuber.pediuber.repository;

import com.pediuber.pediuber.entity.Ride;
import com.pediuber.pediuber.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByStatus(RideStatus status);

}
