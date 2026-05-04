package com.najim.webhookservice.service;


import com.najim.webhookservice.model.PushEvent;
import com.najim.webhookservice.repository.PushEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class WebhookService {

    private final PushEventRepository repository;



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

        // Guard: only handle push events with commits
        List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");
        if (commits == null || commits.isEmpty()) {
            return; // ping event or empty push — skip
        }
            //just mosiba o sf again
        // 1. Extract repoName
        Map<String, Object> repo = (Map<String, Object>) payload.get("repository");
        String repoName = (String) repo.get("full_name");

        // 2. Extract commitSha safely
        Map<String, Object> headCommit = (Map<String, Object>) payload.get("head_commit");
        String commitSha = headCommit != null ? (String) headCommit.get("id") : "unknown";

        // 3. Extract pusher name
        Map<String, Object> pusher = (Map<String, Object>) payload.get("pusher");
        String pusherName = pusher != null ? (String) pusher.get("name") : "unknown";

        // 4. Collect changed files
        List<String> files = new ArrayList<>();
        for (Map<String, Object> commit : commits) {
            List<String> modified = (List<String>) commit.get("modified");
            List<String> added = (List<String>) commit.get("added");
            if (modified != null) files.addAll(modified);
            if (added != null) files.addAll(added);
        }

        // 5. Build and save
        PushEvent event = PushEvent.builder()
                .repoName(repoName)
                .commitSha(commitSha)
                .pusherName(pusherName)
                .changedFiles(files)
                .receivedAt(LocalDateTime.now())
                .build();
        System.out.println("ABOUT TO SAVE - repo: " + repoName + " sha: " + commitSha);
        repository.save(event);


    }
}

