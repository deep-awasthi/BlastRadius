package com.blastradius.dto;

import jakarta.validation.constraints.NotBlank;

public class ScanRequest {

    @NotBlank(message = "Repository path is required")
    private String repoPath;

    private boolean forceRescan = false;

    public ScanRequest() {}

    public ScanRequest(String repoPath) {
        this.repoPath = repoPath;
    }

    public String getRepoPath() { return repoPath; }
    public void setRepoPath(String repoPath) { this.repoPath = repoPath; }

    public boolean isForceRescan() { return forceRescan; }
    public void setForceRescan(boolean forceRescan) { this.forceRescan = forceRescan; }
}
