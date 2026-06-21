package com.najim.generateservice.repository;

import com.najim.generateservice.model.ReviewResult;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewResultRepository extends MongoRepository<ReviewResult, String> {
    List<ReviewResult> findByRepoNameOrderByReceivedAtDesc(String repoName);
    ReviewResult findFirstByRepoNameOrderByReceivedAtDesc(String repoName);
}