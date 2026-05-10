package com.najim.generateservice.controller;

import com.najim.generateservice.dto.PushEventRequest;
import com.najim.generateservice.service.GenerateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/generate")
public class GenerateController {
    public final GenerateService generateService;
    // cathc the payload from webhook-service
    @PostMapping("/caller")
    public ResponseEntity<?> caller(@RequestBody PushEventRequest payload) {
        return ResponseEntity.ok(generateService.HandelPayload(payload));
    }
    // catch the review json from fastapi-service
    @PostMapping("/holler")
    public ResponseEntity<?> holler(@RequestBody Object fastApiResponse) {
        generateService.SendToFront(fastApiResponse);
        return ResponseEntity.ok("send it");
    }
}
