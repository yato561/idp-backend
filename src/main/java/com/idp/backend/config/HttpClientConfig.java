package com.idp.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for HTTP clients
 */
@Configuration
public class HttpClientConfig {

    /**
     * RestClient bean for making HTTP requests
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
