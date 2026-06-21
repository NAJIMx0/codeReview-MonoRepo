package com.najim.generateservice.service;

import com.najim.generateservice.dto.FileReviewRequest;
import com.najim.generateservice.dto.PushEventRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class GenerateService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public List<SseEmitter> getEmitters() {
        return emitters;
    }
    // logs var
    private static final String RESET  = "\033[0m";
    private static final String BOLD   = "\033[1m";
    private static final String GREEN  = "\033[32m";
    private static final String CYAN   = "\033[36m";
    private static final String YELLOW = "\033[33m";
    private static final String RED    = "\033[31m";
    private static final String DIM    = "\033[2m";

    // hardcoced logs
    private void logHeader(String label) {
        System.out.println();
        System.out.println(CYAN + BOLD + "┌─── " + label + " " + "─".repeat(Math.max(0, 50 - label.length())) + RESET);
    }

    private void logField(String key, String value) {
        System.out.println(CYAN + "│  " + RESET + YELLOW + key + RESET + DIM + " → " + RESET + value);
    }

    private void logSuccess(String msg) {
        System.out.println(CYAN + "│  " + RESET + GREEN + "✔  " + msg + RESET);
    }

    private void logWarn(String msg) {
        System.out.println(CYAN + "│  " + RESET + YELLOW + "⚠  " + msg + RESET);
    }

    private void logError(String msg) {
        System.out.println(CYAN + "│  " + RESET + RED + "✖  " + msg + RESET);
    }

    private void logFooter() {
        System.out.println(CYAN + "└" + "─".repeat(55) + RESET);
    }

//    {
//              "username": "NAJIMx0",
//              "accessToken": "gho_xxx",
//              "repoName": "NAJIMx0/my-repo",
//            "changedFiles": ["src/main.py"][][],
//              "commitSha": "abc123"
//    }

    public List<FileReviewRequest> HandelPayload(PushEventRequest payload) {

        logHeader("GENERATE SERVICE — incoming push");
        logField("repo",       payload.repoName());
        logField("pusher",     payload.username());
        logField("commit",     payload.commitSha());
        logField("files",      String.valueOf(payload.changedFiles().size()) + " changed");

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
                logWarn("skipped (IDE/git artifact): " + filepath);
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
                logSuccess("fetched  (" + decoded.length + " bytes)  " + filepath);

            } catch (Exception e) {

                logError("skipped — " + filepath + "  [" + e.getMessage() + "]");
            }
        }

        logField("ready to analyze", ListfileReviewRequests.size() + " / " + payload.changedFiles().size() + " files");
        logFooter();
        if (!ListfileReviewRequests.isEmpty()) {
            sendToFastApi(repoName, ListfileReviewRequests);
        }

//        sendToFastApi(ListfileReviewRequests);
        return ListfileReviewRequests;

    }
    //  Post to orchestrator (FastAPI microservices) /analyze
    private void sendToFastApi(String repoName,List<FileReviewRequest> files) {

        logHeader("GENERATE SERVICE — sending to orchestrator");
        logField("endpoint", "http://orchestrator:8000/analyze");
        logField("files",    String.valueOf(files.size()));

        try {
            // send as {}
            List<Map<String, String>> fileMaps = files.stream()
                    .map(f -> Map.of(
                            "Filename", f.Filename(),
                            "Content",  f.Content()
                    ))
                    .toList();

            Map<String, Object> body = Map.of(
                    "repoName", repoName,
                    "files",    fileMaps
            );

            // sift to orchestrator — it fans out to complexity/style/duplication,
            // then publishes the merged result to Kafka itself (topic: review.result).
            // We don't need to do anything with the HTTP response here anymore —
            // ReviewResultConsumer picks the real result up from Kafka.
            RestClient.create()
                    .post()
                    .uri("http://orchestrator:8000/analyze")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            logSuccess("orchestrator accepted the request");


        } catch (Exception e) {
            logError("orchestrator call failed — " + e.getMessage());
        }
        logFooter();
    }

    public  void sendToFrontViaSse(Object fastApiResponse) {

//        try {
//            RestClient.create()
//                    .post()
//                    .uri("http://localhost:8998/api/generate/holler")
//                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
//                    .body(fastApiResponse)
//                    .retrieve()
//                    .toBodilessEntity();
//            logSuccess("review pushed to SSE stream");
//
//        } catch (Exception e) {
//            logError("SSE push failed — " + e.getMessage());
//        }
//
//        logFooter();
        logHeader("GENERATE SERVICE — pushing review to frontend via SSE");
        logField("connected clients", String.valueOf(emitters.size()));

        if (emitters.isEmpty()) {
            logWarn("no SSE clients connected — is the frontend open on /review ?");
            logFooter();
            return;
        }

        List<SseEmitter> dead = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(fastApiResponse);
                logSuccess("pushed to client");
            } catch (Exception e) {
                logError("client dead, removing — " + e.getMessage());
                dead.add(emitter);
            }
        }

        emitters.removeAll(dead);
        logField("active clients after push", String.valueOf(emitters.size()));
        logFooter();
    }

}