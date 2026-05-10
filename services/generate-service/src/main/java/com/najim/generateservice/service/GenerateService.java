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

            if (filepath.startsWith(".idea") || filepath.startsWith(".git")) {
                continue;
            }
            System.out.println("trying: https://api.github.com/repos/" + repoName + "/contents/" + filepath);
            try {
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
        System.out.println(ListfileReviewRequests);
//        sendToFastApi(ListfileReviewRequests);
        return ListfileReviewRequests;

    }

    private void sendToFastApi(List<FileReviewRequest> files) {
        try {
            RestClient.create()
                    .post()
                    .uri("http://localhost:8181/api/fastapi/input")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(files)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.out.println("FastAPI call failed: " + e.getMessage());
        }
    }

    public  void SendToFront(Object fastApiResponse) {
        try {
            RestClient.create()
                    .post()
                    .uri("http://localhost:5173/review")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(fastApiResponse)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.out.println("FrontEnd call failed: " + e.getMessage());
        }

    }

}
