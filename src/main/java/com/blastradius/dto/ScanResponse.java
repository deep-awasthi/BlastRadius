package com.blastradius.dto;

import com.blastradius.entity.Scan;
import java.time.LocalDateTime;

public class ScanResponse {

    private Long id;
    private String repoPath;
    private String repoName;
    private String status;
    private LocalDateTime scannedAt;
    private LocalDateTime completedAt;
    private Integer totalFiles;
    private Integer totalComponents;
    private Integer totalRelationships;
    private String errorMessage;

    public ScanResponse() {}

    public static ScanResponse from(Scan scan) {
        ScanResponse r = new ScanResponse();
        r.id = scan.getId();
        r.repoPath = scan.getRepoPath();
        r.repoName = scan.getRepoName();
        r.status = scan.getStatus().name();
        r.scannedAt = scan.getScannedAt();
        r.completedAt = scan.getCompletedAt();
        r.totalFiles = scan.getTotalFiles();
        r.totalComponents = scan.getTotalComponents();
        r.totalRelationships = scan.getTotalRelationships();
        r.errorMessage = scan.getErrorMessage();
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRepoPath() { return repoPath; }
    public void setRepoPath(String repoPath) { this.repoPath = repoPath; }

    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getScannedAt() { return scannedAt; }
    public void setScannedAt(LocalDateTime scannedAt) { this.scannedAt = scannedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Integer getTotalFiles() { return totalFiles; }
    public void setTotalFiles(Integer totalFiles) { this.totalFiles = totalFiles; }

    public Integer getTotalComponents() { return totalComponents; }
    public void setTotalComponents(Integer totalComponents) { this.totalComponents = totalComponents; }

    public Integer getTotalRelationships() { return totalRelationships; }
    public void setTotalRelationships(Integer totalRelationships) { this.totalRelationships = totalRelationships; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
