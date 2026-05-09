package com.najim.generateservice.service;

import com.najim.generateservice.dto.FileReviewRequest;
import com.najim.generateservice.dto.PushEventRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GenerateService {

//    {
//              "username": "NAJIMx0",
//              "accessToken": "gho_xxx",
//              "repoName": "NAJIMx0/my-repo",
//            "changedFiles": ["src/main.py"][][],
//              "commitSha": "abc123"
//    }

    public List<FileReviewRequest> HandelPayload(PushEventRequest payload) {
        List<String> changedFiles = payload.changedFiles();
        String repoName = payload.repoName();
        String username = payload.username();
        String accessToken = payload.accessToken();
        String commitSha = payload.commitSha();

        List<FileReviewRequest> ListfileReviewRequests = new ArrayList<>();

        for(String filepath : changedFiles) {

            if (filepath.startsWith(".idea") || filepath.startsWith(".git")) {
                continue;
            }
            System.out.println("trying: https://api.github.com/repos/" + repoName + "/contents/" + filepath);
            try {
                Map<String, Object> response = RestClient.create()
                        .get()
                        .uri("https://api.github.com/repos/{repoName}/contents/{filePath}", repoName, filepath)
                        .header("Authorization", "Bearer " + accessToken)
                        .retrieve()
                        .body(Map.class);
                String encoded = (String) response.get("content");
                byte[] decoded = Base64.getDecoder().decode(encoded.replaceAll("\\n", ""));
                String contentCode = new String(decoded);
                ListfileReviewRequests.add(new FileReviewRequest(filepath, contentCode));
            } catch (Exception e) {
                System.out.println("skipping file: " + filepath + " reason: " + e.getMessage());
            }
        }
        System.out.println(ListfileReviewRequests);
        return ListfileReviewRequests;

    }
}
