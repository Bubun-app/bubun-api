package com.bubun.gocardless.configuration;

import com.bubun.gocardless.api.TokenApi;
import com.bubun.gocardless.client.ApiClient;
import com.bubun.gocardless.service.GoCardlessTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GoCardlessProperties.class)
public class GoCardlessApiAutoConfiguration {

    @Bean
    public ApiClient apiClient(GoCardlessProperties properties, GoCardlessTokenService tokenService) {
        ApiClient client = new ApiClient();
        client.setBasePath(properties.apiUrl());
        client.setBearerToken(tokenService.getValidAccessToken());
        return client;
    }

    @Bean
    public TokenApi tokenApi(GoCardlessProperties properties) {
        ApiClient client = new ApiClient();
        client.setBasePath(properties.apiUrl());
        return new TokenApi(client);
    }
}
