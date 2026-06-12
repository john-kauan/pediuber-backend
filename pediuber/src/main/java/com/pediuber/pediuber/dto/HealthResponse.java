package com.pediuber.pediuber.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {

    private String status;
    private long availableDrivers;
    private int pendingRides;
    private double averageLatency;
    private int inputQueueMessages;
    private int outputQueueMessages;
    private int inputDlqMessages;
    private int outputDlqMessages;
}
