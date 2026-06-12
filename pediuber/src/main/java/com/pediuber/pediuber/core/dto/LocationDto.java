package com.pediuber.pediuber.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {

    private double lat;

    private double lng;

    private String street;

    private String number;

    private String city;

    private String state;

}
