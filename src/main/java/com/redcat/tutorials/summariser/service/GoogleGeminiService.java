package com.redcat.tutorials.summariser.service;

import com.redcat.tutorials.summariser.dto.gemini.GeminiRequest;
import com.redcat.tutorials.summariser.dto.gemini.GeminiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class GoogleGeminiService {

    private final RestTemplate restTemplate;
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String GEMINI_MODEL = "gemini-2.5-flash-preview-05-20";

    // Rate limiting - limit to 10 concurrent requests
    private final Semaphore rateLimiter = new Semaphore(10);
    private static final long RATE_LIMIT_DELAY_MS = 10000;

    List<String> apiKeys = List.of("AIzaSyDMaXp_k6T-UaSM7-EptFfZAyE2gvYx1iw");

    private final AtomicInteger keyCounter = new AtomicInteger(0);

    @Value("${google.api.key}")
    private String defaultApiKey;

    public GoogleGeminiService() {
        this.restTemplate = new RestTemplate();
    }

    private String getNextApiKey() {
        int currentIndex = keyCounter.getAndUpdate(current -> (current + 1) % apiKeys.size());
        String selectedKey = apiKeys.get(currentIndex);
        log.info("Using API key {} of {}", currentIndex + 1, apiKeys.size());
        return selectedKey;
    }

    public Mono<GeminiResponse> generateContent(String ignoredApiKey, String prompt) {
        String activeApiKey = getNextApiKey();
        log.info("Selected API key for request: index {}", keyCounter.get());

        GeminiRequest request = createRequest(prompt);
        String url = String.format("%s/models/%s:generateContent?key=%s", GEMINI_BASE_URL, GEMINI_MODEL, activeApiKey);

        return Mono.fromCallable(() -> {
                    boolean acquired = rateLimiter.tryAcquire(30, TimeUnit.SECONDS);
                    if (!acquired) {
                        log.warn("Failed to acquire rate limiter permit after 30 seconds");
                        throw new RuntimeException("Rate limit reached, try again later");
                    }
                    Thread.sleep(RATE_LIMIT_DELAY_MS); // Delay between requests

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);
                    ResponseEntity<GeminiResponse> responseEntity = restTemplate.postForEntity(url, entity, GeminiResponse.class);

                    if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                        throw new RestClientException("Non-successful status code: " + responseEntity.getStatusCode());
                    }

                    return responseEntity.getBody();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .retryWhen(createRetrySpec())
                .doFinally(signal -> rateLimiter.release())
                .onErrorResume(e -> {
                    log.error("Failed to generate content with API key index {} after all retries: {}",
                            keyCounter.get(), e.getMessage());
                    return Mono.error(e);
                });
    }

    private RetryBackoffSpec createRetrySpec() {
        return Retry.backoff(10, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofMinutes(2))
                .jitter(0.5)
                .filter(this::isRetryableException)
                .doBeforeRetry(retrySignal -> {
                    String nextApiKey = getNextApiKey();
                    long retryCount = retrySignal.totalRetries() + 1;
                    log.warn("Retry attempt {} using API key index {}. Error: {}",
                            retryCount, keyCounter.get(), retrySignal.failure().getMessage());
                })
                .onRetryExhaustedThrow((spec, signal) -> {
                    log.error("Failed after {} retry attempts", signal.totalRetries());
                    return signal.failure();
                });
    }

    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof RestClientException) {
            // For simplicity, assume RestClientException means retryable (you can refine this)
            return true;
        }
        return true;
    }

    private GeminiRequest createRequest(String prompt) {
        GeminiRequest.Part part = new GeminiRequest.Part(prompt);
        GeminiRequest.Content content = new GeminiRequest.Content(Collections.singletonList(part));
        return new GeminiRequest(Collections.singletonList(content));
    }

    public String extractResponseText(GeminiResponse response) {
        if (response != null &&
                response.getCandidates() != null &&
                !response.getCandidates().isEmpty() &&
                response.getCandidates().get(0).getContent() != null &&
                response.getCandidates().get(0).getContent().getParts() != null &&
                !response.getCandidates().get(0).getContent().getParts().isEmpty()) {

            return response.getCandidates().get(0).getContent().getParts().get(0).getText();
        }
        return "";
    }
}
