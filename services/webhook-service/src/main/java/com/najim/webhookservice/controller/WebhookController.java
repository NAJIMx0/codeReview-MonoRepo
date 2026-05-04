package com.najim.webhookservice.controller;

import com.najim.webhookservice.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {
    private final WebhookService webhookService;

    @PostMapping("/github")
    public ResponseEntity<String> handlePush(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestBody Map<String, Object> payload
    ) {
        if (!event.equals("push")) {
            return ResponseEntity.ok("ignored"); // ping and other events
        }

        try {
            webhookService.process(payload);
            return ResponseEntity.ok("received");
        } catch (Exception e) {
            e.printStackTrace(); // check your console
            return ResponseEntity.status(500).body("error: " + e.getMessage());
        }
    }
}
