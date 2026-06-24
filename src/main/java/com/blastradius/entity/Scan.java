package com.blastradius.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a repository scan operation and its metadata.
 */
@Entity
@Table(name = "scans")
public class Scan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_path", nullable = false, length = 1000)
    private String repoPath;

    @Column(name = "repo_name")
    private String repoName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScanStatus status = ScanStatus.PENDING;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_files")
    private Integer totalFiles = 0;

    @Column(name = "total_components")
    private Integer totalComponents = 0;

    @Column(name = "total_relationships")
    private Integer totalRelationships = 0;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        scannedAt = LocalDateTime.now();
    }

    public enum ScanStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }

    // Constructors
    public Scan() {}

    public Scan(String repoPath) {
        this.repoPath = repoPath;
        this.repoName = extractRepoName(repoPath);
    }

    private String extractRepoName(String path) {
        if (path == null || path.isBlank()) return "unknown";
        String cleaned = path.replace("\\", "/").trim();
        if (cleaned.endsWith("/")) cleaned = cleaned.substring(0, cleaned.length() - 1);
        int idx = cleaned.lastIndexOf('/');
        return idx >= 0 ? cleaned.substring(idx + 1) : cleaned;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRepoPath() { return repoPath; }
    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
        this.repoName = extractRepoName(repoPath);
    }

    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }

    public ScanStatus getStatus() { return status; }
    public void setStatus(ScanStatus status) { this.status = status; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
