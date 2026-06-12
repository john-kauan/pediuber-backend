package com.pediuber.pediuber.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CoreClientConfig {

    @Bean
    public RestClient coreRestClient(
            @Value("${ridefleet.core.base-url}") String coreBaseUrl,
            @Value("${ridefleet.api-key:}") String apiKey
    ) {

        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(coreBaseUrl);

        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("X-API-Key", apiKey);
        }

        return builder.build();
    }
}