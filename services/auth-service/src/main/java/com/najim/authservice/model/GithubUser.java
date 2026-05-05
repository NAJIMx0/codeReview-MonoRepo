package com.najim.authservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="github_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubUser {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String githubId;
    private String username;
    private String email;
    private String avatarUrl;
    private String accessToken;
    private LocalDateTime createdAt;

}
