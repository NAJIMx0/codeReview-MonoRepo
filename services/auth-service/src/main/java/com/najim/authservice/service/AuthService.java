package com.najim.authservice.service;

import com.najim.authservice.model.GithubUser;
import com.najim.authservice.repository.GithubUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AuthService {

    private final GithubUserRepository githubUserRepository;

    public GithubUser HandleLogin(OAuth2AuthenticationToken token) {
        Map<String,Object> attributes = token.getPrincipal().getAttributes();

        String githubId = String.valueOf(attributes.get("id"));
        String username = (String) attributes.get("login");
        String email = attributes.get("email")!=null ?attributes.get("email").toString():"private";// check email
        String avatarUrl = (String) attributes.get("avatar_url");

        Optional<GithubUser> userExist = githubUserRepository.findByGithubId(githubId);

        if(userExist.isPresent()){
            return userExist.get();
        }else{
            GithubUser userCr = GithubUser.builder()
                    .githubId(githubId)
                    .username(username)
                    .email(email)
                    .avatarUrl(avatarUrl)
                    .createdAt(LocalDateTime.now())
                    .build();
            githubUserRepository.save(userCr);
            return userCr;
        }

    }
}
