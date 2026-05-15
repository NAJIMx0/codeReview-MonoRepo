package com.najim.generateservice.service;

import com.najim.generateservice.dto.FileReviewRequest;
import com.najim.generateservice.dto.PushEventRequest;
import org.jspecify.annotations.Nullable;
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
//        String username = payload.username();
        String accessToken = payload.accessToken();
//        String commitSha = payload.commitSha();

        List<FileReviewRequest> ListfileReviewRequests = new ArrayList<>();
        System.out.println("token received: " + accessToken);
        for(String filepath : changedFiles) {
            // skipi had .idea , .git  . no test

            if (filepath.startsWith(".idea") || filepath.startsWith(".git")) {
                continue;
            }
            if (!filepath.endsWith(".py")) continue;

            System.out.println("trying: https://api.github.com/repos/" + repoName + "/contents/" + filepath);
            try {
                // sift req bach takod code
                Map<String, Object> response = RestClient.create()
                        .get()
                        .uri("https://api.github.com/repos/" + repoName + "/contents/" + filepath)
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
        if (!ListfileReviewRequests.isEmpty()) {
            sendToFastApi(repoName, ListfileReviewRequests);
        }

        System.out.println(ListfileReviewRequests);
//        sendToFastApi(ListfileReviewRequests);
        return ListfileReviewRequests;

    }
        //  Post to FastAPI /analyze
    private void sendToFastApi(String repoName,List<FileReviewRequest> files) {
        try {
            Map<String, Object> body = Map.of(
                    "repoName", repoName,
                    "files", files
            );
            // sift to analyse service
            Object fastApiResponse = RestClient.create()
                    .post()
                    .uri("http://fastapi-service:8181/analyze")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            System.out.println("FastAPI response received, forwarding to SSE...");
            sendToFrontViaSse(fastApiResponse);

        } catch (Exception e) {
            System.out.println("FastAPI call failed: " + e.getMessage());
        }
    }

    public  void sendToFrontViaSse(Object fastApiResponse) {
        try {
            RestClient.create()
                    .post()
                    .uri("http://localhost:8998/api/generate/holler")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(fastApiResponse)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.out.println("SSE failed: " + e.getMessage());
        }

    }

}
