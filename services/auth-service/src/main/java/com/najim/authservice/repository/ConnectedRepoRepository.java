package com.najim.authservice.repository;

import com.najim.authservice.model.ConnectedRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConnectedRepoRepository extends JpaRepository<ConnectedRepo, Long> {
    List<ConnectedRepo> findByGithubId(String githubId);
    boolean existsByGithubIdAndRepoName(String githubId, String repoName);
}