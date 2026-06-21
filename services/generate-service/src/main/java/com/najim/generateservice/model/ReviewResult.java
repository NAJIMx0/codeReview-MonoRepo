package com.najim.generateservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "review_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResult {

    @Id
    private String id;

    private String repoName;
    private Map<String, Object> payload;   // the full JSON from orchestrator/AI service
    private LocalDateTime receivedAt;
}