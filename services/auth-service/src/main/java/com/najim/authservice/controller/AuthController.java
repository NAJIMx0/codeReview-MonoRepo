package com.najim.authservice.controller;

import com.najim.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @GetMapping()
    public ResponseEntity<?> successLogin(OAuth2AuthenticationToken token){
        authService.HandleLogin(token);
        return ResponseEntity.ok("logged in bro ");
    }
    @GetMapping("/repo")
    public ResponseEntity<?> getRepo(OAuth2AuthenticationToken token){
        return ResponseEntity.ok(authService.getGithuhRepo(token));
    }

    @PostMapping("/connect/{owner}/{repoName}")
    public ResponseEntity<?> connectRepo(
            @PathVariable String owner,
            @PathVariable String repoName,
            OAuth2AuthenticationToken token) {
        return ResponseEntity.ok(authService.connectRepo(owner, repoName, token));
    }

    @GetMapping("/me")
    public ResponseEntity<?> userInfo(OAuth2AuthenticationToken token){
        Map<String,Object> userdata  =token.getPrincipal().getAttributes();
        String username = (String) userdata.get("login");
        return ResponseEntity.ok(username);
    }

}
