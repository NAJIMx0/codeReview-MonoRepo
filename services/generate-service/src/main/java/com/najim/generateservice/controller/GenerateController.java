package com.najim.generateservice.controller;

import com.najim.generateservice.dto.PushEventRequest;
import com.najim.generateservice.service.GenerateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/generate")
public class GenerateController {
    public final GenerateService generateService;

    @PostMapping("/review")
    public ResponseEntity<?> review(@RequestBody PushEventRequest request) {
        return ResponseEntity.ok(generateService.processReview(request));
    }
}
