package com.najim.authservice.controller;

import com.najim.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @GetMapping("/success")
    public ResponseEntity<String> successLogin(OAuth2AuthenticationToken token){
        authService.HandleLogin(token);
        return ResponseEntity.ok("logged in");
    }

    @GetMapping("/me")
    public ResponseEntity<String> userInfo(OAuth2AuthenticationToken token){
        Map<String,Object> userdata  =token.getPrincipal().getAttributes();
        String username = (String) userdata.get("login");
        return ResponseEntity.ok(username);
    }

}
