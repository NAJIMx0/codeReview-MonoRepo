package com.najim.authservice.controller;

import com.najim.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @GetMapping("/success")
    public void successLogin(OAuth2AuthenticationToken token,
                                            HttpServletResponse response) throws IOException {
        authService.HandleLogin(token);
        response.sendRedirect("http://localhost:5173/dashboard");
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
    @GetMapping("/connected-repos")
    public ResponseEntity<?> getConnectedRepos(OAuth2AuthenticationToken token) {
        return ResponseEntity.ok(authService.getConnectedRepos(token));
    }
    @GetMapping("/me")
    public ResponseEntity<?> userInfo(OAuth2AuthenticationToken token){
        Map<String,Object> userdata  =token.getPrincipal().getAttributes();
        String username = (String) userdata.get("login");
        return ResponseEntity.ok(username);
    }
    @PostMapping("/revoke")
    public ResponseEntity<?> revokeToken(OAuth2AuthenticationToken token) {
        if (token != null) {
            authService.revokeGithubToken(token);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/token/{username}")
    public ResponseEntity<String> getTokenByUsername(@PathVariable String username) {
        return ResponseEntity.ok(authService.getTokenByUsername(username));
    }

}
