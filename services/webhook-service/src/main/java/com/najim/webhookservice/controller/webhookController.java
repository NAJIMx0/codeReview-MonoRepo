package com.najim.webhookservice.controller;

import com.najim.webhookservice.service.webhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class webhookController {

   private  webhookService webhookService;

   @PostMapping("/api/webhook/github")
    public ResponseEntity<Map<String,String>> githubWebhook(

   ){
       return ResponseEntity.ok(Map.of("status", "received"));
   }

}
