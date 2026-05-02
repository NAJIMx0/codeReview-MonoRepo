package com.najim.webhookservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "push_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushEvent {

    @Id
    private String id;

    private String repoName;
    private String commitSha;
    private String pusherName;
    private List<String> changedFiles;
    private LocalDateTime receivedAt;
}