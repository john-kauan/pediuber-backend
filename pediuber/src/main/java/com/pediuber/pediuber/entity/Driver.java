package com.pediuber.pediuber.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String vehicle;

    private Boolean available;

    public Driver(String name, String vehicle) {
        this.name = name;
        this.vehicle = vehicle;
        this.available = true;
    }

}
