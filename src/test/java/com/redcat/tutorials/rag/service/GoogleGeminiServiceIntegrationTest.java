package com.redcat.tutorials.rag.service;

import com.redcat.tutorials.summariser.dto.gemini.GeminiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class GoogleGeminiServiceIntegrationTest {

    @Autowired
    private GoogleGeminiService googleGeminiService;

    @Value("${google.api.key}")
    private String apiKey;

    @Mock
    private WebClient.Builder webClientBuilder;

    private WebClient webClientMock;
    private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;
    private WebClient.RequestBodySpec requestBodySpecMock;
    private WebClient.RequestHeadersSpec requestHeadersSpecMock;
    private WebClient.ResponseSpec responseSpecMock;

    @BeforeEach
    public void setup() {
        // Setup mocks
        webClientMock = mock(WebClient.class);
        requestBodyUriSpecMock = mock(WebClient.RequestBodyUriSpec.class);
        requestBodySpecMock = mock(WebClient.RequestBodySpec.class);
        requestHeadersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        responseSpecMock = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.baseUrl(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClientMock);
        when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        when(requestBodyUriSpecMock.uri(any(), any(), any())).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.bodyValue(any())).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);

        // Recreate the service with our mocked WebClient
        googleGeminiService = new GoogleGeminiService(webClientBuilder);
    }

    @Test
    public void testGenerateContent_Success() {
        // Prepare mock response
        GeminiResponse mockResponse = createMockGeminiResponse();
        when(responseSpecMock.bodyToMono(GeminiResponse.class)).thenReturn(Mono.just(mockResponse));

        // Execute the service method
        String prompt = "Explain how AI works in a few words";
        Mono<GeminiResponse> responseMono = googleGeminiService.generateContent(apiKey, prompt);

        // Verify the response
        GeminiResponse response = responseMono.block();
        assertThat(response).isNotNull();
        assertThat(response.getCandidates()).isNotEmpty();
        assertThat(response.getCandidates().get(0).getContent().getParts().get(0).getText())
            .isEqualTo("AI allows computers to learn from data, recognize patterns, and make intelligent decisions or predictions, essentially mimicking human thought.");
    }

    @Test
    public void testExtractResponseText() {
        // Create a mock response
        GeminiResponse mockResponse = createMockGeminiResponse();

        // Call the method being tested
        String extractedText = googleGeminiService.extractResponseText(mockResponse);

        // Verify the extracted text matches the expected output
        assertThat(extractedText).isEqualTo("AI allows computers to learn from data, recognize patterns, and make intelligent decisions or predictions, essentially mimicking human thought.");
    }

    private GeminiResponse createMockGeminiResponse() {
        GeminiResponse response = new GeminiResponse();

        // Create candidate
        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate();
        candidate.setFinishReason("STOP");
        candidate.setIndex(0);

        // Create content
        GeminiResponse.Content content = new GeminiResponse.Content();
        content.setRole("model");

        // Create part
        GeminiResponse.Part part = new GeminiResponse.Part();
        part.setText("AI allows computers to learn from data, recognize patterns, and make intelligent decisions or predictions, essentially mimicking human thought.");

        // Link everything together
        List<GeminiResponse.Part> parts = new ArrayList<>();
        parts.add(part);
        content.setParts(parts);
        candidate.setContent(content);

        List<GeminiResponse.Candidate> candidates = new ArrayList<>();
        candidates.add(candidate);
        response.setCandidates(candidates);

        // Add usage metadata
        GeminiResponse.UsageMetadata usageMetadata = new GeminiResponse.UsageMetadata();
        usageMetadata.setPromptTokenCount(8);
        usageMetadata.setCandidatesTokenCount(26);
        usageMetadata.setTotalTokenCount(1053);
        usageMetadata.setThoughtsTokenCount(1019);

        List<GeminiResponse.PromptTokensDetail> promptTokensDetails = new ArrayList<>();
        GeminiResponse.PromptTokensDetail detail = new GeminiResponse.PromptTokensDetail();
        detail.setModality("TEXT");
        detail.setTokenCount(8);
        promptTokensDetails.add(detail);
        usageMetadata.setPromptTokensDetails(promptTokensDetails);

        response.setUsageMetadata(usageMetadata);
        response.setModelVersion("models/gemini-2.5-flash-preview-05-20");
        response.setResponseId("V5g5aNS9NvjVz7IPqbzasQY");

        return response;
    }
}
