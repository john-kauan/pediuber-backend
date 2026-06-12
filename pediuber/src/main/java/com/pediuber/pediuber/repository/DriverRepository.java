package com.pediuber.pediuber.repository;

import com.pediuber.pediuber.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findByAvailableTrue();

    Optional<Driver> findFirstByAvailableTrue();

    boolean existsByAvailableTrue();

    long countByAvailableTrue();
}
