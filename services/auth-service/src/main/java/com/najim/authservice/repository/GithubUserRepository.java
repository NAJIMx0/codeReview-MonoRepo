package com.najim.authservice.repository;

import com.najim.authservice.model.GithubUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface  GithubUserRepository extends JpaRepository<GithubUser,Long> {
    //kayna id table o kayn githubId
    Optional<GithubUser> findByGithubId(String githubId);
}
