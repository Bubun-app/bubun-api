package com.bubun.bubunapi.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bubun")
public record AppProperties(String url) {}
