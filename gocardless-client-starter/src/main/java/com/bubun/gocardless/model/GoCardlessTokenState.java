package com.bubun.gocardless.model;

import java.time.Instant;

public record GoCardlessTokenState(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {}

