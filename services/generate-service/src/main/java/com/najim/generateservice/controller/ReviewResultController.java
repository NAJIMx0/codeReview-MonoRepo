package com.najim.generateservice.controller;

import com.najim.generateservice.model.ReviewResult;
import com.najim.generateservice.repository.ReviewResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewResultController {

    private final ReviewResultRepository reviewResultRepository;

    // repo is passed as a query param (?repo=NAJIMx0/test-repo), not a path
    // variable — repo names contain a slash, and Tomcat rejects encoded
    // slashes (%2F) in path segments by default. Query params don't have
    // this restriction, so this sidesteps the issue entirely.

    // Full history for a repo, most recent first.
    @GetMapping("/history")
    public ResponseEntity<List<ReviewResult>> getHistory(@RequestParam String repo) {
        return ResponseEntity.ok(reviewResultRepository.findByRepoNameOrderByReceivedAtDesc(repo));
    }

    // Just the latest result — what the Dashboard card shows.
    @GetMapping("/latest")
    public ResponseEntity<ReviewResult> getLatest(@RequestParam String repo) {
        ReviewResult latest = reviewResultRepository.findFirstByRepoNameOrderByReceivedAtDesc(repo);
        if (latest == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(latest);
    }
}