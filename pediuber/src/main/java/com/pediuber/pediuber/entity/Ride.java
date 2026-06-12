package com.pediuber.pediuber.entity;


import com.pediuber.pediuber.enums.RideStatus;
import jakarta.persistence.*;
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

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne
    @JoinColumn(name = "passenger_id")
    private Passenger passenger;

    @Enumerated(EnumType.STRING)
    private RideStatus status;

    private LocalDateTime createdAt;

    @Column(unique = true)
    private String coreRideUuid;

    private String externalPassengerId;

    private String originServiceId;

    private String lockExpiresAt;

    private Long logicalTimestamp;

    public Ride(String origin, String destination, Passenger passenger) {
        this.origin = origin;
        this.destination = destination;
        this.passenger = passenger;
        this.status = RideStatus.REQUESTED;
        this.createdAt = LocalDateTime.now();
    }
}