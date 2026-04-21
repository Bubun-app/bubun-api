package com.bubun.gocardless.service;

import com.bubun.gocardless.api.TokenApi;
import com.bubun.gocardless.configuration.GoCardlessProperties;
import com.bubun.gocardless.exception.GoCardlessTokenException;
import com.bubun.gocardless.model.GoCardlessRetryListener;
import com.bubun.gocardless.model.GoCardlessTokenState;
import com.bubun.gocardless.model.SpectacularJWTObtain;
import com.bubun.gocardless.model.SpectacularJWTRefresh;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoCardlessTokenServiceTest {

    @Mock
    private TokenApi api;

    private GoCardlessTokenService service;

    @BeforeEach
    void setup() {
        var props = new GoCardlessProperties(
                "http://localhost:8080",
                "secret-id",
                "secret-key"
        );
        service = new GoCardlessTokenService(api, props, new GoCardlessRetryListener());
    }

    @Test
    void shouldCreateNewTokenWhenNoneExists() {
        var response = new SpectacularJWTObtain("access123", 3600, "refresh123", 300);

        when(api.obtainNewAccessRefreshTokenPair(any())).thenReturn(Mono.just(response));

        var token = service.getValidAccessToken();

        assertEquals("access123", token);
    }

    @Test
    void shouldRefreshAccessTokenWhenExpired() {
        var initialState = new GoCardlessTokenState(
                "oldAccess",
                Instant.now().minusSeconds(10),
                "refresh123",
                Instant.now().plusSeconds(300)
        );

        ReflectionTestUtils.setField(service, "state", initialState);

        var refreshResponse = new SpectacularJWTRefresh("newAccess", 3600);

        when(api.getANewAccessToken(any())).thenReturn(Mono.just(refreshResponse));

        var token = service.getValidAccessToken();

        assertEquals("newAccess", token);
    }

    @Test
    void shouldFallbackToCreateTokenWhenRefreshFails() {
        var initialState = new GoCardlessTokenState(
                "oldAccess",
                Instant.now().minusSeconds(10),
                "refresh123",
                Instant.now().plusSeconds(300)
        );

        ReflectionTestUtils.setField(service, "state", initialState);

        when(api.getANewAccessToken(any())).thenThrow(new RestClientException("invalid refresh"));

        var createResponse = new SpectacularJWTObtain("newAccess", 3600, "newRefresh", 300);

        when(api.obtainNewAccessRefreshTokenPair(any())).thenReturn(Mono.just(createResponse));

        var token = service.getValidAccessToken();

        assertEquals("newAccess", token);
    }

    @Test
    void shouldFallbackToCreateWhenRefreshReturnsNull() {
        var initialState = new GoCardlessTokenState(
                "oldAccess",
                Instant.now().minusSeconds(10),
                "refresh123",
                Instant.now().plusSeconds(300)
        );

        ReflectionTestUtils.setField(service, "state", initialState);

        when(api.getANewAccessToken(any())).thenReturn(Mono.empty());

        var createResponse = new SpectacularJWTObtain("newAccess", 3600, "newRefresh", 300);
        when(api.obtainNewAccessRefreshTokenPair(any())).thenReturn(Mono.just(createResponse));

        var token = service.getValidAccessToken();

        assertEquals("newAccess", token);
        verify(api, times(1)).getANewAccessToken(any()); // NO retry
    }

    @Test
    void shouldFailImmediatelyWhenCreateReturnsNull() {
        when(api.obtainNewAccessRefreshTokenPair(any())).thenReturn(Mono.empty());

        assertThrows(GoCardlessTokenException.class, () -> service.getValidAccessToken());

        verify(api, times(1)).obtainNewAccessRefreshTokenPair(any()); // NO retry
    }

    @Test
    void shouldNotCallApiWhenAccessTokenValid() {
        var initialState = new GoCardlessTokenState(
                "validAccess",
                Instant.now().plusSeconds(300),
                "refresh123",
                Instant.now().plusSeconds(300)
        );

        ReflectionTestUtils.setField(service, "state", initialState);

        var token = service.getValidAccessToken();

        assertEquals("validAccess", token);
        verifyNoInteractions(api);
    }

    @Test
    void shouldUseTokenRefreshedByAnotherThread() throws Exception {
        // Initial state: expired access token, valid refresh token
        var initialState = new GoCardlessTokenState(
                "oldAccess",
                Instant.now().minusSeconds(10),
                "refresh123",
                Instant.now().plusSeconds(300)
        );

        ReflectionTestUtils.setField(service, "state", initialState);

        var refreshResponse = new SpectacularJWTRefresh("newAccess", 3600);

        var refreshCalls = new AtomicInteger(0);

        when(api.getANewAccessToken(any())).thenAnswer(_ -> {
            refreshCalls.incrementAndGet();
            return Mono.just(refreshResponse);
        });

        var threadCount = 20;
        var futures = new ArrayList<Future<String>>();

        try (var executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(service::getValidAccessToken));
            }

            // Awaitility waits for all futures to complete
            Awaitility.await()
                    .atMost(Duration.ofSeconds(2))
                    .until(() -> futures.stream().allMatch(Future::isDone));
        }

        // Collect results
        var results = new ArrayList<String>();
        for (Future<String> f : futures) {
            results.add(f.get());
        }

        // All should receive the new token
        assertThat(results).allMatch(token -> token.equals("newAccess"));

        // And the refresh should have been called only once
        assertThat(refreshCalls.get()).isEqualTo(1);
    }

    @Test
    void shouldRetryCreateFourTimesOn429() {
        var ex = new WebClientResponseException(
                429,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );
        when(api.obtainNewAccessRefreshTokenPair(any())).thenThrow(ex);

        assertThrows(GoCardlessTokenException.class, () -> service.getValidAccessToken());

        verify(api, times(5)).obtainNewAccessRefreshTokenPair(any());
    }

    @Test
    void shouldNotRetryCreateOn400Or401() {
        var initialState = new GoCardlessTokenState(
                "oldAccess",
                Instant.now().minusSeconds(10),
                "refresh123",
                Instant.now().plusSeconds(300)
        );

        ReflectionTestUtils.setField(service, "state", initialState);

        var ex = new WebClientResponseException(
                400,
                "Bad Request",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );
        when(api.obtainNewAccessRefreshTokenPair(any())).thenThrow(ex);

        assertThrows(GoCardlessTokenException.class, () -> service.getValidAccessToken());

        verify(api, times(1)).obtainNewAccessRefreshTokenPair(any()); // NO retry
    }

    @Test
    void shouldNotRetryCreateOn401() {
        var ex = new WebClientResponseException(
                401,
                "Unauthorized",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );

        when(api.obtainNewAccessRefreshTokenPair(any())).thenThrow(ex);

        assertThrows(GoCardlessTokenException.class, () -> service.getValidAccessToken());

        verify(api, times(1)).obtainNewAccessRefreshTokenPair(any()); // NO retry
    }

    @Test
    void shouldRetryCreateFourTimesOn500() {
        when(api.obtainNewAccessRefreshTokenPair(any()))
                .thenThrow(new WebClientResponseException(500, "Server Error", null, null, null));

        assertThrows(GoCardlessTokenException.class, () -> service.getValidAccessToken());

        verify(api, times(5)).obtainNewAccessRefreshTokenPair(any());
    }

    @Test
    void shouldRetryRefreshFourTimesOn429() {
        var initialState = new GoCardlessTokenState(
                "oldAccess",
                Instant.now().minusSeconds(10),
                "refresh123",
                Instant.now().plusSeconds(300)
        );

        ReflectionTestUtils.setField(service, "state", initialState);

        when(api.getANewAccessToken(any()))
                .thenThrow(new WebClientResponseException(429, "Too Many Requests", null, null, null));

        assertThrows(GoCardlessTokenException.class, () -> service.getValidAccessToken());

        verify(api, times(5)).getANewAccessToken(any()); // 1 + 4 retries
    }

    @Test
    void shouldNotRetryRefreshOn400Or401() {
        var initialState = new GoCardlessTokenState(
                "oldAccess",
                Instant.now().minusSeconds(10),
                "refresh123",
                Instant.now().plusSeconds(300)
        );

        ReflectionTestUtils.setField(service, "state", initialState);

        when(api.getANewAccessToken(any()))
                .thenThrow(new WebClientResponseException(400, "Bad Request", null, null, null));

        var createResponse = new SpectacularJWTObtain("newAccess", 3600, "newRefresh", 300);
        when(api.obtainNewAccessRefreshTokenPair(any())).thenReturn(Mono.just(createResponse));

        var token = service.getValidAccessToken();

        assertEquals("newAccess", token);
        verify(api, times(1)).getANewAccessToken(any()); // NO retry
    }

}