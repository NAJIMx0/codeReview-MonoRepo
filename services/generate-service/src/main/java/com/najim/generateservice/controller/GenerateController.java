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

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
//end point sse
    @GetMapping("/stream")
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
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
        for (SseEmitter emitter : emitters) {
            try {
                // send it to front
                emitter.send(fastApiResponse);
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
        return ResponseEntity.ok("send it");
    }
}
