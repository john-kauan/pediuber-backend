package com.pediuber.pediuber.dto;

import com.pediuber.pediuber.enums.RideStatus;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UpdateRideStatusRequest {

    private RideStatus status;

}
