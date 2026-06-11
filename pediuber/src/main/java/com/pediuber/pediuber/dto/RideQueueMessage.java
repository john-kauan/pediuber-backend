package com.pediuber.pediuber.dto;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RideQueueMessage {

    private Long rideId;

    private String origin;

    private String destination;

    private Long passengerId;

    private String status;

    private String enqueuedAt;

    private int attempts;


}
