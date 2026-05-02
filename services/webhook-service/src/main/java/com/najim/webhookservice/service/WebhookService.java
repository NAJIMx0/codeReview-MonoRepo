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


    public   void process(Map<String, Object> payload){
        // 1. extract repoName
        Map<String,Object> repo = (Map<String, Object>) payload.get("repository");
        String RepoName = (String) repo.get("full_name");

        // 2. extract commitSha
        Map<String,Object> headCommit = (Map<String, Object>) payload.get("head_commit");
        String CommitSha = (String) headCommit.get("id");
        // 3. loop commits, collect files
        List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");

        List<String> Files = new ArrayList<>();

        for (Map<String, Object> commit : commits) {
            List<String> modified = (List<String>) commit.get("modified");
            List<String> added = (List<String>) commit.get("added");
            Files.addAll(modified);
            Files.addAll(added);
        }
        // 4. build PushEvent
        PushEvent event = PushEvent.builder()
                .repoName(RepoName)
                .commitSha(CommitSha)
                .changedFiles(Files)
                .receivedAt(LocalDateTime.now())
                .build();


        // 5. repository.save(pushEvent)
        repository.save(event);
    }
}

