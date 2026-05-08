package com.najim.generateservice.dto;

import java.util.List;

public record PushEventRequest(
        String repoName,
        String commitSha,
        String pusherName,
        List<String> changedFiles
) {}