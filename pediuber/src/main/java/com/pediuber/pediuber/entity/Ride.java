package com.pediuber.pediuber.entity;


import com.pediuber.pediuber.enums.RideStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rides")
@Getter
@Setter
@NoArgsConstructor
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String origin;

    private String destination;

    private Long passengerId;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Enumerated(EnumType.STRING)
    private RideStatus status;

    private LocalDateTime createdAt;

    public Ride(String origin, String destination, Long passengerId) {
        this.origin = origin;
        this.destination = destination;
        this.passengerId = passengerId;
        this.status = RideStatus.REQUESTED;
        this.createdAt = LocalDateTime.now();
    }
}