package com.najim.generateservice.controller;

import com.najim.generateservice.dto.PushEventRequest;
import com.najim.generateservice.service.GenerateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/generate")
public class GenerateController {
    public final GenerateService generateService;

    //end point sse
    @GetMapping("/stream")
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        generateService.getEmitters().add(emitter);
        emitter.onCompletion(() -> generateService.getEmitters().remove(emitter));
        emitter.onTimeout(()    -> generateService.getEmitters().remove(emitter));
        return emitter;
    }
    // cathc the payload from webhook-service
    @PostMapping("/caller")
    public ResponseEntity<?> caller(@RequestBody PushEventRequest payload) {
        return ResponseEntity.ok(generateService.HandelPayload(payload));
    }

    // catch the review json from fastapi-service
    @PostMapping("/holler")
    public ResponseEntity<?> holler(@RequestBody Object fastApiResponse) {
        generateService.sendToFrontViaSse(fastApiResponse);
        return ResponseEntity.ok("sent");
    }
}
