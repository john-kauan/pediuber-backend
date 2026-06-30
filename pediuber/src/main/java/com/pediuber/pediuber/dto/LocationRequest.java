package com.pediuber.pediuber.dto;

public record LocationRequest(
    Double lat,
    Double lng,
    String street,
    String number,
    String city,
    String state
){
}
