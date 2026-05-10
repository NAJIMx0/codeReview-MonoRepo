package com.najim.webhookservice.service;


import com.najim.webhookservice.config.RestClientConfig;
import com.najim.webhookservice.dto.pusherYYPayload;
import com.najim.webhookservice.model.PushEvent;
import com.najim.webhookservice.repository.PushEventRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class WebhookService {

    private final PushEventRepository repository;
    private final RestClient.Builder  restClientbuilder;


//    {
//        "ref": "refs/heads/main",
//            "repository": {
//        "id": 123456,
//                "full_name": "NAJIMx0/my-repo",
//                "private": false
//    },
//        "pusher": {
//        "name": "NAJIMx0",
//                "email": "najim@example.com"
//    },
//        "head_commit": {
//        "id": "abc123def456",
//                "message": "fix bug in main",
//                "timestamp": "2024-01-01T12:00:00Z",
//                "author": {
//            "name": "Najim",
//                    "email": "najim@example.com"
//        }
//    },
//        "commits": [
//        {
//            "id": "abc123def456",
//                "message": "fix bug in main",
//                "added":    ["src/newfile.py"],
//            "modified": ["src/main.py", "utils/helper.js"],
//            "removed":  ["old/trash.java"]
//        }
//  ]
//    }


    public void process(Map<String, Object> payload) {

        System.out.println("SERVICE CALLED - files: " + payload.get("commits"));


        List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");
        if (commits == null || commits.isEmpty()) {
            return; // ping event or empty push — skip it  brovv
        }

        //just mosiba o sf again alah lmostaan my borthee --naah just fetch easy alah with us
        Map<String, Object> repo = (Map<String, Object>) payload.get("repository");
        String repoName = (String) repo.get("full_name");

        Map<String, Object> headCommit = (Map<String, Object>) payload.get("head_commit");
        String commitSha = headCommit != null ? (String) headCommit.get("id") : "unknown";

        Map<String, Object> pusher = (Map<String, Object>) payload.get("pusher");
        String pusherName = pusher != null ? (String) pusher.get("name") : "unknown";

        List<String> files = new ArrayList<>();
        for (Map<String, Object> commit : commits) {
            List<String> modified = (List<String>) commit.get("modified");
            List<String> added = (List<String>) commit.get("added");
            if (modified != null) files.addAll(modified);
            if (added != null) files.addAll(added);
        }

        PushEvent event = PushEvent.builder()
                .repoName(repoName)
                .commitSha(commitSha)
                .pusherName(pusherName)
                .changedFiles(files)
                .receivedAt(LocalDateTime.now())
                .build();

        repository.save(event);

        String accessToken = ProvideAccessT(pusherName);
        System.out.println("accessToken: " + accessToken);

        restClientbuilder.build()
                .post()
                .uri("http://localhost:8998/api/generate/caller")
                .body(Map.of(
                        "username", pusherName,
                        "accessToken", accessToken,
                        "repoName", repoName,
                        "changedFiles", files,
                        "commitSha", commitSha
                ))
                .retrieve()
                .toBodilessEntity();


    }

    public String ProvideAccessT(String username) {

        return restClientbuilder.build()
                .get()
                .uri("http://localhost:8080/api/auth/token/{username}", username)
                .retrieve()
                .body(String.class);
    }

}

