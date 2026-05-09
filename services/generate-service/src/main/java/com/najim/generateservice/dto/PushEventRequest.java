package com.najim.generateservice.dto;

import java.util.List;

public record PushEventRequest(
        String username,
        String accessToken,
        String repoName,
        List<String> changedFiles,
        String commitSha
) {}