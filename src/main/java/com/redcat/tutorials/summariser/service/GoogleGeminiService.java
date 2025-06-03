package com.redcat.tutorials.summariser.service;

import com.redcat.tutorials.summariser.dto.gemini.GeminiRequest;
import com.redcat.tutorials.summariser.dto.gemini.GeminiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
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

    private final WebClient webClient;
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String GEMINI_MODEL = "gemini-2.5-flash-preview-05-20";

    // Rate limiting - limit to 10 concurrent requests
    private final Semaphore rateLimiter = new Semaphore(10);
    // Configurable rate limit delay in ms (default: 10000ms between requests)
    private static final long RATE_LIMIT_DELAY_MS = 10000;

    List<String> apiKeys = List.of("AIzaSyDMaXp_k6T-UaSM7-EptFfZAyE2gvYx1iw");


    // Counter for round-robin API key selection
    private final AtomicInteger keyCounter = new AtomicInteger(0);

    @Value("${google.api.key}")
    private String defaultApiKey;

    @Autowired
    public GoogleGeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(GEMINI_BASE_URL).build();
    }

    /**
     * Gets the next API key using round-robin selection
     * @return The next API key to use
     */
    private String getNextApiKey() {
        int currentIndex = keyCounter.getAndUpdate(current -> (current + 1) % apiKeys.size());
        String selectedKey = apiKeys.get(currentIndex);
        log.info("Using API key {} of {}", currentIndex + 1, apiKeys.size());
        return selectedKey;
    }

    /**
     * Generate content using Google Gemini API with exponential retry and rate limiting
     *
     * @param ignoredApiKey Google API key from config (not used as we're using round-robin from our list)
     * @param prompt The text prompt to send to the model
     * @return Mono<GeminiResponse> containing the API response
     */
    public Mono<GeminiResponse> generateContent(String ignoredApiKey, String prompt) {
        // Get the next API key using round-robin
        String activeApiKey = getNextApiKey();
        log.info("Selected API key for request: index {}", keyCounter.get());

        // Create request body based on the curl example
        GeminiRequest request = createRequest(prompt);

        return Mono.fromCallable(() -> {
            // Apply rate limiting
            try {
                boolean acquired = rateLimiter.tryAcquire(30, TimeUnit.SECONDS);
                if (!acquired) {
                    log.warn("Failed to acquire rate limiter permit after 30 seconds");
                    throw new RuntimeException("Rate limit reached, try again later");
                }
                // Add delay between requests to avoid bursts
                Thread.sleep(RATE_LIMIT_DELAY_MS);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Rate limiting interrupted", e);
            }
        })
        .flatMap(acquired -> webClient.post()
                .uri("/models/{model}:generateContent?key={key}", GEMINI_MODEL, activeApiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class))
        .retryWhen(createRetrySpec())
        .doFinally(signal -> rateLimiter.release())
        .onErrorResume(e -> {
            log.error("Failed to generate content with API key index {} after all retries: {}",
                    keyCounter.get(), e.getMessage());
            return Mono.error(e);
        });
    }

    /**
     * Creates a retry specification with exponential backoff for handling rate limits.
     * @return RetryBackoffSpec configured for optimal handling of rate limits
     */
    private RetryBackoffSpec createRetrySpec() {
        return Retry.backoff(10, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofMinutes(2))
                .jitter(0.5)
                .filter(this::isRetryableException)
                .doBeforeRetry(retrySignal -> {
                    // On retry, get a new API key using round-robin
                    String nextApiKey = getNextApiKey();
                    long retryCount = retrySignal.totalRetries() + 1;

                    if (retrySignal.failure() instanceof WebClientResponseException) {
                        WebClientResponseException wcre = (WebClientResponseException) retrySignal.failure();
                        log.warn("API call failed with status {}. Retry attempt {} using API key index {}. Error: {}",
                                wcre.getStatusCode(), retryCount, keyCounter.get(), wcre.getMessage());
                    } else {
                        log.warn("API call failed. Retry attempt {} using API key index {}. Error: {}",
                                retryCount, keyCounter.get(), retrySignal.failure().getMessage());
                    }
                })
                .onRetryExhaustedThrow((spec, signal) -> {
                    log.error("Failed after {} retry attempts", signal.totalRetries());
                    return signal.failure();
                });
    }

    /**
     * Determines if an exception should trigger a retry attempt
     * @param throwable The exception to evaluate
     * @return true if the exception is retryable, false otherwise
     */
    private boolean isRetryableException(Throwable throwable) {
        // Always retry on rate limiting (429)
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) throwable;
            HttpStatus status = HttpStatus.valueOf(wcre.getStatusCode().value());

            // Retry on rate limits (429) and server errors (5xx)
            if (status.equals(HttpStatus.TOO_MANY_REQUESTS) || status.is5xxServerError()) {
                return true;
            }

            // Don't retry on client errors except rate limits
            return false;
        }

        // Retry on network errors and other exceptions
        return true;
    }

    /**
     * Create a request object for the Gemini API
     *
     * @param prompt The text prompt to send to the model
     * @return GeminiRequest object
     */
    private GeminiRequest createRequest(String prompt) {
        GeminiRequest.Part part = new GeminiRequest.Part(prompt);
        GeminiRequest.Content content = new GeminiRequest.Content(Collections.singletonList(part));
        return new GeminiRequest(Collections.singletonList(content));
    }

    /**
     * Extract text from the Gemini response
     *
     * @param response The Gemini API response
     * @return The generated text or empty string if no valid response
     */
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
