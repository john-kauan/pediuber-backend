package com.pediuber.pediuber.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRideRequest {

    private String origin;
    private String destination;
    private Long passengerId;

}
