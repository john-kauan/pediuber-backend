package com.pediuber.pediuber.repository;

import com.pediuber.pediuber.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {
}
