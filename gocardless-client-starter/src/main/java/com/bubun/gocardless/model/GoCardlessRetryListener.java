package com.bubun.gocardless.model;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.Retryable;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class GoCardlessRetryListener implements RetryListener {

    private final AtomicInteger totalRetries = new AtomicInteger(0);
    private final AtomicInteger successfulRecoveries = new AtomicInteger(0);
    private final AtomicInteger finalFailures = new AtomicInteger(0);

    private final ThreadLocal<Integer> currentAttempt = ThreadLocal.withInitial(() -> 0);

    @Override
    public void beforeRetry(@NonNull RetryPolicy retryPolicy,
                            Retryable<?> retryable) {
        int attemptNumber = currentAttempt.get() + 1;
        currentAttempt.set(attemptNumber);
        totalRetries.incrementAndGet();
        log.info("RetryListener: Attempt #{} starting for operation '{}'",
                attemptNumber,
                retryable.getName());
    }

    @Override
    public void onRetrySuccess(@NonNull RetryPolicy retryPolicy,
                               @NonNull Retryable<?> retryable,
                               Object result) {
        int attemptCount = currentAttempt.get();

        if (attemptCount > 1) {
            successfulRecoveries.incrementAndGet();
            log.info("RetryListener: Operation '{}' succeeded after {} attempt(s)",
                    retryable.getName(),
                    attemptCount);
        }

        currentAttempt.remove();
    }

    @Override
    public void onRetryFailure(@NonNull RetryPolicy retryPolicy,
                               Retryable<?> retryable,
                               Throwable throwable) {
        int attemptCount = currentAttempt.get();
        finalFailures.incrementAndGet();
        log.error("RetryListener: Operation '{}' failed after {} attempt(s): {}",
                retryable.getName(),
                attemptCount,
                throwable.getMessage());
    }
}
