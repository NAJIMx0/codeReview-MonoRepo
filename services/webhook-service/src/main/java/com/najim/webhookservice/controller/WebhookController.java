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
        // call webhookService.process(payload)
        webhookService.process(payload);
        return ResponseEntity.ok("received");
    }
}
