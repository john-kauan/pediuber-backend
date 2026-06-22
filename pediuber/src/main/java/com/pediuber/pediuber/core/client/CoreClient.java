package com.pediuber.pediuber.core.client;

import com.pediuber.pediuber.core.dto.RideAccepted;
import com.pediuber.pediuber.core.dto.RideRequestToCore;
import com.pediuber.pediuber.core.dto.RideStatusUpdateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class CoreClient {

    private final RestClient coreRestClient;
    private final String apiKey;
    private final String coreBaseUrl;
    private final HttpClient httpClient;

    public CoreClient(
            RestClient coreRestClient,
            @Value("${ridefleet.api-key:}") String apiKey,
            @Value("${ridefleet.core.base-url}") String coreBaseUrl
    ) {
        this.coreRestClient = coreRestClient;
        this.apiKey = apiKey;
        this.coreBaseUrl = removeTrailingSlash(coreBaseUrl);
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public String updateRideStatus(
            String rideUuid,
            RideStatusUpdateRequest request
    ) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new RestClientException("RIDEFLEET_API_KEY is empty");
        }

        if (request.newState() == null || request.newState().isBlank()) {
            throw new RestClientException("newState is empty");
        }

        if (request.serviceId() == null || request.serviceId().isBlank()) {
            throw new RestClientException("serviceId is empty");
        }

        if (request.logicalTimestamp() == null) {
            throw new RestClientException("logicalTimestamp is null");
        }

        String jsonBody = """
                {
                  "newState": "%s",
                  "serviceId": "%s",
                  "logicalTimestamp": %d
                }
                """.formatted(
                escapeJson(request.newState()),
                escapeJson(request.serviceId()),
                request.logicalTimestamp()
        );

        String url = coreBaseUrl + "/rides/" + rideUuid + "/status";

        System.out.println("[CORE_PATCH_URL] " + url);
        System.out.println("[CORE_PATCH_BODY] " + jsonBody);

        try {

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-API-Key", apiKey)
                    .method(
                            "PATCH",
                            HttpRequest.BodyPublishers.ofString(
                                    jsonBody,
                                    StandardCharsets.UTF_8
                            )
                    )
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("[CORE_PATCH_RESPONSE_STATUS] " + response.statusCode());
            System.out.println("[CORE_PATCH_RESPONSE_BODY] " + response.body());

            if (response.statusCode() >= 400) {
                throw new RestClientException(
                        "Core status update failed: HTTP "
                                + response.statusCode()
                                + " - "
                                + response.body()
                );
            }

            return response.body();

        } catch (IOException exception) {

            throw new RestClientException(
                    "Failed to send status update to Core: " + exception.getMessage(),
                    exception
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new RestClientException(
                    "Interrupted while sending status update to Core: " + exception.getMessage(),
                    exception
            );
        }
    }

    public RideAccepted createRide(RideRequestToCore request) {

        return coreRestClient
                .post()
                .uri("/rides")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-API-Key", apiKey)
                .body(request)
                .retrieve()
                .body(RideAccepted.class);
    }

    private String removeTrailingSlash(String url) {

        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }

        return url;
    }

    private String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}