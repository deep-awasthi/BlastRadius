package com.blastradius.controller;

import com.blastradius.dto.ApiResponse;
import com.blastradius.git.GitHistoryAnalyzer;
import com.blastradius.git.GitHistoryAnalyzer.GitReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Git intelligence queries.
 */
@RestController
@RequestMapping("/api/git")
@Tag(name = "Git Intelligence", description = "Git history analysis: contributors, bus factor, co-changes")
@SecurityRequirement(name = "bearerAuth")
public class GitController {

    private final GitHistoryAnalyzer gitHistoryAnalyzer;

    public GitController(GitHistoryAnalyzer gitHistoryAnalyzer) {
        this.gitHistoryAnalyzer = gitHistoryAnalyzer;
    }

    @GetMapping("/analyze")
    @Operation(summary = "Analyze git history of a local repository path")
    public ResponseEntity<ApiResponse<GitReport>> analyzeGit(
            @RequestParam String repoPath) {
        GitReport report = gitHistoryAnalyzer.analyze(repoPath);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
