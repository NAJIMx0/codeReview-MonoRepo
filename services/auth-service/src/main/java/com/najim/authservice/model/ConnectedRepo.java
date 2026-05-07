package com.najim.authservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "connected_repos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectedRepo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String githubId;
    private String repoName;
    private String owner;
}