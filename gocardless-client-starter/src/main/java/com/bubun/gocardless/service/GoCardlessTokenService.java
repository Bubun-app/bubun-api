package com.bubun.gocardless.service;

import com.bubun.gocardless.api.TokenApi;
import com.bubun.gocardless.configuration.GoCardlessProperties;
import com.bubun.gocardless.exception.GoCardlessTokenException;
import com.bubun.gocardless.model.GoCardlessRetryListener;
import com.bubun.gocardless.model.GoCardlessTokenState;
import com.bubun.gocardless.model.JWTObtainPairRequest;
import com.bubun.gocardless.model.JWTRefreshRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
public class GoCardlessTokenService {

    private static final int MAX_RETRY_ATTEMPTS = 4;
    private static final Duration MAX_RETRY_DELAY_SECONDS = Duration.ofSeconds(1);
    private static final double RETRY_MULTIPLIER = 2.0;
    private static final int DEFAULT_TOKEN_EXPIRY_BUFFER_SECONDS = 30; // Buffer time before actual expiry to consider token as expired

    private final TokenApi api;
    private final String secretId;
    private final String secretKey;
    private final RetryTemplate retryTemplate;
    private final Object lock = new Object();

    private volatile GoCardlessTokenState state;

    public GoCardlessTokenService(TokenApi api, GoCardlessProperties properties, GoCardlessRetryListener retryListener) {
        this.api = api;
        this.secretId = properties.secretId();
        this.secretKey = properties.secretKey();
        this.retryTemplate = createRetryTemplate(retryListener);
    }

    public String getValidAccessToken() {
        var currentState = state; // Read volatile state into local variable for thread safety

        // First check without lock (optimistic)
        if (isAccessTokenValid(currentState)) {
            log.debug("Using cached access token");
            return currentState.accessToken();
        }

        // Token is missing or expired, acquire lock and double-check
        synchronized (lock) {
            currentState = state; // Re-read state after acquiring lock
            if (isAccessTokenValid(currentState)) {
                log.debug("Token was refreshed by another thread");
                return currentState.accessToken();
            }
            refreshOrCreateToken(currentState);
            return state.accessToken();
        }
    }

    private void refreshOrCreateToken(GoCardlessTokenState currentState) {
        // Try refresh first if we have a valid refresh token
        if (isRefreshTokenValid(currentState)) {
            try {
                log.info("Attempting to refresh GoCardless access token using refresh token");
                state = retryTemplate.execute(() -> refreshTokenState(currentState));
                log.info("Successfully refreshed access token");
                return;
            } catch (RetryException e) {
                log.warn("Failed to refresh token, falling back to full authentication: {}", e.getMessage());
            } catch (Exception e) {
                log.warn("Failed to refresh token, falling back to full authentication", e);
            }
        }

        // Full auth (new token pair)
        try {
            log.info("Attempting to obtain new token pair from GoCardless");
            state = retryTemplate.execute(this::createTokenState);
            log.info("Successfully obtained new token pair");
        } catch (RetryException e) {
            log.error("Failed to obtain new token pair: {}", e.getCause().getMessage());
            throw new GoCardlessTokenException("Failed to obtain access token", e);
        } catch (Exception e) {
            log.error("Failed to obtain new token pair", e);
            throw new GoCardlessTokenException("Failed to obtain access token", e);
        }
    }

    private GoCardlessTokenState refreshTokenState(GoCardlessTokenState currentState) {
        var request = new JWTRefreshRequest().refresh(currentState.refreshToken());
        var response = api.getANewAccessToken(request).block();

        if (response == null || response.getAccess() == null || response.getAccessExpires() == null) {
            log.warn("Refresh token invalid or expired. Not retryable.");
            throw new GoCardlessTokenException("Invalid token refresh response: response is null or missing access token");
        }

        var newState = new GoCardlessTokenState(
                response.getAccess(),
                Instant.now().plusSeconds(response.getAccessExpires()),
                currentState.refreshToken(),
                currentState.refreshTokenExpiresAt()
        );

        log.debug("Access token refreshed, expires at: {}", newState.accessTokenExpiresAt());
        return newState;
    }

    private GoCardlessTokenState createTokenState() {
        var request = new JWTObtainPairRequest()
                .secretId(secretId)
                .secretKey(secretKey);

        var response = api.obtainNewAccessRefreshTokenPair(request).block();

        if (response == null || response.getAccess() == null || response.getRefresh() == null
                || response.getAccessExpires() == null || response.getRefreshExpires() == null) {
            log.warn("Invalid credentials. Not retryable.");
            throw new GoCardlessTokenException("Invalid token pair response: response is null or missing access token");
        }

        var now = Instant.now();
        var newState = new GoCardlessTokenState(
                response.getAccess(),
                now.plusSeconds(response.getAccessExpires()),
                response.getRefresh(),
                now.plusSeconds(response.getRefreshExpires())
        );

        log.debug("New token pair obtained, access token expires at: {}, refresh token expires at: {}",
                newState.accessTokenExpiresAt(), newState.refreshTokenExpiresAt());
        return newState;
    }

    private boolean isAccessTokenValid(GoCardlessTokenState status) {
        return status != null && isTokenValid(status.accessToken(), status.accessTokenExpiresAt());
    }

    private boolean isRefreshTokenValid(GoCardlessTokenState status) {
        return status != null && isTokenValid(status.refreshToken(), status.refreshTokenExpiresAt());
    }

    private boolean isTokenValid(String token, Instant expiresAt) {
        return token != null && expiresAt != null
                && !Instant.now().isAfter(expiresAt.minusSeconds(DEFAULT_TOKEN_EXPIRY_BUFFER_SECONDS));
    }

    private boolean isRetryableException(Throwable ex) {
        if (ex instanceof WebClientResponseException wcre) {
            int status = wcre.getStatusCode().value();
            return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
        }
        return false;
    }

    private RetryTemplate createRetryTemplate(GoCardlessRetryListener retryListener) {
        var retryPolicy = RetryPolicy.builder()
                .maxRetries(MAX_RETRY_ATTEMPTS)
                .delay(MAX_RETRY_DELAY_SECONDS)
                .multiplier(RETRY_MULTIPLIER)
                .predicate(this::isRetryableException)
                .build();
        var template = new RetryTemplate(retryPolicy);
        template.setRetryListener(retryListener);
        return template;
    }
}