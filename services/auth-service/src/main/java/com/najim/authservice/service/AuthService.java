package com.najim.authservice.service;

import com.najim.authservice.model.GithubUser;
import com.najim.authservice.repository.GithubUserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AuthService{

private final GithubUserRepository githubUserRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;

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
                .body(List.class);
    }

    public  Object connectRepo(String owner, String repoName, OAuth2AuthenticationToken token) {
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


    }
}