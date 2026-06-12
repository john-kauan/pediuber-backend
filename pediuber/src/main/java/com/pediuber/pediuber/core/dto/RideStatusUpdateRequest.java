package com.pediuber.pediuber.core.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RideStatusUpdateRequest {

    private String newState;

    private String serviceId;

    private long logicalTimestamp;

}
