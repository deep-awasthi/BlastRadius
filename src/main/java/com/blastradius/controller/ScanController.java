package com.blastradius.controller;

import com.blastradius.dto.ApiResponse;
import com.blastradius.dto.ScanRequest;
import com.blastradius.dto.ScanResponse;
import com.blastradius.service.ScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for triggering and querying repository scans.
 */
@RestController
@RequestMapping("/api/repositories")
@Tag(name = "Repository Scanner", description = "Trigger and manage code repository scans")
@SecurityRequirement(name = "bearerAuth")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping("/scan")
    @Operation(summary = "Scan a local git repository")
    public ResponseEntity<ApiResponse<ScanResponse>> scan(
            @Valid @RequestBody ScanRequest request) {
        ScanResponse response = scanService.initiateScan(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "Scan initiated — running asynchronously"));
    }

    @GetMapping("/scans")
    @Operation(summary = "List all scans")
    public ResponseEntity<ApiResponse<List<ScanResponse>>> getAllScans() {
        return ResponseEntity.ok(ApiResponse.success(scanService.getAllScans()));
    }

    @GetMapping("/scans/{scanId}")
    @Operation(summary = "Get a specific scan by ID")
    public ResponseEntity<ApiResponse<ScanResponse>> getScan(@PathVariable Long scanId) {
        return ResponseEntity.ok(ApiResponse.success(scanService.getScan(scanId)));
    }
}
