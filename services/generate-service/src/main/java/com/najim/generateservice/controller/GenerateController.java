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

    @PostMapping("/caller")
    public ResponseEntity<?> caller(@RequestBody PushEventRequest payload) {
        return ResponseEntity.ok(generateService.HandelPayload(payload));
    }

}
