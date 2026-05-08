package com.najim.authservice.service;

import com.najim.authservice.model.ConnectedRepo;
import com.najim.authservice.model.GithubUser;
import com.najim.authservice.repository.ConnectedRepoRepository;
import com.najim.authservice.repository.GithubUserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AuthService{

private final GithubUserRepository githubUserRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final ConnectedRepoRepository connectedRepoRepository;

    public GithubUser HandleLogin(OAuth2AuthenticationToken token) {
        //  access token
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                token.getAuthorizedClientRegistrationId(),
                token.getName()
        );
        String accessToken = client.getAccessToken().getTokenValue();

        Map<String, Object> attributes = token.getPrincipal().getAttributes();
        String githubId = String.valueOf(attributes.get("id"));
        String username = (String) attributes.get("login");
        String email = attributes.get("email") != null ? attributes.get("email").toString() : "private";
        String avatarUrl = (String) attributes.get("avatar_url");

        Optional<GithubUser> userExist = githubUserRepository.findByGithubId(githubId);

        if (userExist.isPresent()) {
            GithubUser existing = userExist.get();
            existing.setAccessToken(accessToken);
            return githubUserRepository.save(existing);
        } else {
            GithubUser userCr = GithubUser.builder()
                    .githubId(githubId)
                    .username(username)
                    .accessToken(accessToken)
                    .email(email)
                    .avatarUrl(avatarUrl)
                    .createdAt(LocalDateTime.now())
                    .build();
            return githubUserRepository.save(userCr);
        }
    }

    public List<Map<String,Object>> getGithuhRepo(OAuth2AuthenticationToken token) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                token.getAuthorizedClientRegistrationId(),
                token.getName()
        );
        String accessToken = client.getAccessToken().getTokenValue();

        RestClient restClient = RestClient.create();
        return restClient.get()
                .uri("https://api.github.com/user/repos")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(List.class);// list of mapat
    }

    public  Map<String, Object> connectRepo(String owner, String repoName, OAuth2AuthenticationToken token) {
        Map<String, Object> attributes = token.getPrincipal().getAttributes();
        String githubId = String.valueOf(attributes.get("id"));

        try {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    token.getAuthorizedClientRegistrationId(),
                    token.getName()
            );
            String accessToken = client.getAccessToken().getTokenValue();

            Map<String, Object> config = Map.of(
                    "url", "https://bronco-revival-marathon.ngrok-free.dev/api/webhook/github",
                    "content_type", "json"
            );
            Map<String, Object> body = Map.of(
                    "config", config,
                    "events", List.of("push"),
                    "active", true
            );

            RestClient restClient = RestClient.create();
            return restClient.post()
                    .uri("https://api.github.com/repos/" + owner + "/" + repoName + "/hooks")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

        }catch (Exception e){
            System.out.println(e.getMessage()+"allredy exist");
        }

        if (!connectedRepoRepository.existsByGithubIdAndRepoName(githubId, repoName)) {
            connectedRepoRepository.save(ConnectedRepo.builder()
                    .githubId(githubId)
                    .repoName(repoName)
                    .owner(owner)
                    .build());
        }

        return Map.of("status", "connected", "repo", repoName);

    }

    public List<String> getConnectedRepos(OAuth2AuthenticationToken token) {
        Map<String, Object> attributes = token.getPrincipal().getAttributes();
        String githubId = String.valueOf(attributes.get("id"));
        return connectedRepoRepository.findByGithubId(githubId)
                .stream()
                .map(ConnectedRepo::getRepoName)
                .toList();
    }

    public void revokeGithubToken(OAuth2AuthenticationToken token) {
        try {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    token.getAuthorizedClientRegistrationId(),
                    token.getName()
            );
            if (client == null) return;

            String accessToken = client.getAccessToken().getTokenValue();
            String credentials = Base64.getEncoder().encodeToString(
                    "Ov23li6ROKOgMYOPi707:f45edbc188ee561f73f0b886625e0f8a427448a4".getBytes()
            );

            RestClient.create()
                    .method(HttpMethod.DELETE)
                    .uri("https://api.github.com/applications/{client_id}/token",
                            "Ov23li6ROKOgMYOPi707")
                    .header("Authorization", "Basic " + credentials)
                    .header("Accept", "application/vnd.github+json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("access_token", accessToken))
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {
            // Never block logout even if revocation fails
            System.out.println("Token revocation failed (non-critical): " + e.getMessage());
        }
    }
}