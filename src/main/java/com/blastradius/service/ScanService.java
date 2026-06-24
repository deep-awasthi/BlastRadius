package com.blastradius.service;

import com.blastradius.analysis.ArchitectureDriftDetector;
import com.blastradius.analysis.DeadCodeDetector;
import com.blastradius.analysis.RiskAnalyzer;
import com.blastradius.dto.ScanRequest;
import com.blastradius.dto.ScanResponse;
import com.blastradius.entity.Scan;
import com.blastradius.entity.Scan.ScanStatus;
import com.blastradius.exception.ResourceNotFoundException;
import com.blastradius.exception.ScanException;
import com.blastradius.repository.ScanRepository;
import com.blastradius.scanner.JavaSourceScanner;
import com.blastradius.scanner.JavaSourceScanner.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates repository scanning, analysis, and persistence.
 */
@Service
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private final ScanRepository scanRepository;
    private final JavaSourceScanner javaSourceScanner;
    private final RiskAnalyzer riskAnalyzer;
    private final DeadCodeDetector deadCodeDetector;
    private final ArchitectureDriftDetector driftDetector;

    public ScanService(ScanRepository scanRepository,
                       JavaSourceScanner javaSourceScanner,
                       RiskAnalyzer riskAnalyzer,
                       DeadCodeDetector deadCodeDetector,
                       ArchitectureDriftDetector driftDetector) {
        this.scanRepository = scanRepository;
        this.javaSourceScanner = javaSourceScanner;
        this.riskAnalyzer = riskAnalyzer;
        this.deadCodeDetector = deadCodeDetector;
        this.driftDetector = driftDetector;
    }

    /**
     * Initiate a scan — creates a PENDING scan record and runs it asynchronously.
     */
    @Transactional
    public ScanResponse initiateScan(ScanRequest request) {
        Scan scan = new Scan(request.getRepoPath());
        scan.setStatus(ScanStatus.PENDING);
        Scan saved = scanRepository.save(scan);
        log.info("Initiated scan #{} for: {}", saved.getId(), request.getRepoPath());

        // Kick off async scan
        performScanAsync(saved.getId());

        return ScanResponse.from(saved);
    }

    /**
     * Asynchronous scan execution: parse files, build relationships, run analyses.
     */
    @Async
    public void performScanAsync(Long scanId) {
        Scan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan", scanId));

        scan.setStatus(ScanStatus.RUNNING);
        scanRepository.save(scan);

        try {
            log.info("Starting async scan #{}", scanId);
            ScanResult result = javaSourceScanner.scan(scan.getRepoPath(), scan);

            scan.setTotalFiles(result.fileCount());
            scan.setTotalComponents(result.componentCount());
            scan.setTotalRelationships(result.relationshipCount());
            scan.setStatus(ScanStatus.COMPLETED);
            scan.setCompletedAt(LocalDateTime.now());
            scanRepository.save(scan);

            // Post-scan analysis
            log.info("Running post-scan analysis for scan #{}", scanId);
            riskAnalyzer.analyzeAndPersist(scanId);
            deadCodeDetector.detectAndMark(scanId);
            // Architecture drift is on-demand; no persistence needed

            log.info("Scan #{} completed: {} files, {} components, {} relationships",
                    scanId, result.fileCount(), result.componentCount(), result.relationshipCount());

        } catch (Exception e) {
            log.error("Scan #{} failed: {}", scanId, e.getMessage(), e);
            scan.setStatus(ScanStatus.FAILED);
            scan.setErrorMessage(e.getMessage());
            scan.setCompletedAt(LocalDateTime.now());
            scanRepository.save(scan);
        }
    }

    @Transactional(readOnly = true)
    public ScanResponse getScan(Long scanId) {
        return ScanResponse.from(scanRepository.findById(scanId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan", scanId)));
    }

    @Transactional(readOnly = true)
    public List<ScanResponse> getAllScans() {
        return scanRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ScanResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScanResponse getLatestCompletedScan() {
        return scanRepository.findByStatus(ScanStatus.COMPLETED).stream()
                .findFirst()
                .map(ScanResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No completed scans found"));
    }
}
