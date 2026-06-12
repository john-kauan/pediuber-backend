package com.pediuber.pediuber.core.client;

import com.pediuber.pediuber.core.dto.RideStatusUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CoreClient {

    private final RestClient coreRestClient;

    public CoreClient(RestClient coreRestClient) {
        this.coreRestClient = coreRestClient;
    }

    public String updateRideStatus(
            String rideUuid,
            RideStatusUpdateRequest request
    ) {

        return coreRestClient
                .patch()
                .uri("/rides/{rideUuid}/status", rideUuid)
                .body(request)
                .retrieve()
                .body(String.class);
    }
}