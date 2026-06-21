package com.najim.generateservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.najim.generateservice.model.ReviewResult;
import com.najim.generateservice.repository.ReviewResultRepository;
import com.najim.generateservice.service.GenerateService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Listens on "review.result" (raw analysis from orchestrator) AND
 * "review.result.ai" (same payload + ai_review field, from the Groq/Llama
 * service). Both get pushed live via SSE so the /review page can show
 * "analyzing..." then "done" if it wants to. Only the AI-enriched final
 * version gets saved permanently to Mongo — the raw intermediate one is
 * just a live-progress signal, not something worth keeping in history.
 */
@Component
@RequiredArgsConstructor
public class ReviewResultConsumer {

    private final GenerateService generateService;
    private final ReviewResultRepository reviewResultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "review.result", groupId = "generate-service-group")
    public void onReviewResult(String message) {
        handle(message, "review.result", false);
    }

    @KafkaListener(topics = "review.result.ai", groupId = "generate-service-group")
    public void onAiReviewResult(String message) {
        handle(message, "review.result.ai", true);
    }

    private void handle(String message, String topic, boolean saveToHistory) {
        System.out.println("KAFKA — received " + topic + " message (" + message.length() + " chars)");
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);

            if (saveToHistory) {
                String repoName = String.valueOf(payload.getOrDefault("repo", "unknown"));
                reviewResultRepository.save(
                        ReviewResult.builder()
                                .repoName(repoName)
                                .payload(payload)
                                .receivedAt(LocalDateTime.now())
                                .build()
                );
            }

            generateService.sendToFrontViaSse(payload);
        } catch (Exception e) {
            System.out.println("KAFKA — failed to parse/forward " + topic + " — " + e.getMessage());
        }
    }
}