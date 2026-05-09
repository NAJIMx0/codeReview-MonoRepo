package com.najim.webhookservice.dto;

import java.util.List;

public record pusherYYPayload (
        String accessT,
        String repoName,
         String commitSha,
         String pusherName,
         List<String>changedFiles
) {}
