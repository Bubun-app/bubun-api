package com.bubun.gocardless.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gocardless")
public record GoCardlessProperties (
        String apiUrl,
        String secretId,
        String secretKey
) { }

