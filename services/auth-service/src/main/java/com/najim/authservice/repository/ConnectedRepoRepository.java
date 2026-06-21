package com.najim.authservice.repository;

import com.najim.authservice.model.ConnectedRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConnectedRepoRepository extends JpaRepository<ConnectedRepo, Long> {
    List<ConnectedRepo> findByGithubId(String githubId);
    boolean existsByGithubIdAndRepoName(String githubId, String repoName);
    Optional<ConnectedRepo> findByGithubIdAndRepoName(String githubId, String repoName);
}