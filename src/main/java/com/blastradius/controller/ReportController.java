package com.blastradius.controller;

import com.blastradius.analysis.ArchitectureDriftDetector.ArchitectureViolation;
import com.blastradius.dto.ApiResponse;
import com.blastradius.dto.ComponentNodeDto;
import com.blastradius.dto.RelationshipDto;
import com.blastradius.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for report generation.
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Generate dependency, risk, and architecture reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dependency")
    @Operation(summary = "Get full dependency (relationship) report for a scan")
    public ResponseEntity<ApiResponse<List<RelationshipDto>>> getDependencyReport(
            @RequestParam Long scanId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getDependencyReport(scanId)));
    }

    @GetMapping("/risk")
    @Operation(summary = "Get risk report ordered by risk score descending")
    public ResponseEntity<ApiResponse<List<ComponentNodeDto>>> getRiskReport(
            @RequestParam Long scanId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getRiskReport(scanId)));
    }

    @GetMapping("/architecture")
    @Operation(summary = "Get architecture drift/violation report for a scan")
    public ResponseEntity<ApiResponse<List<ArchitectureViolation>>> getArchitectureReport(
            @RequestParam Long scanId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getArchitectureReport(scanId)));
    }
}
