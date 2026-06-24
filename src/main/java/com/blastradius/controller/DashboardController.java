package com.blastradius.controller;

import com.blastradius.dto.ApiResponse;
import com.blastradius.dto.DashboardDto;
import com.blastradius.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for dashboard metrics.
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Aggregated dependency intelligence metrics")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Get aggregated dashboard metrics for a scan")
    public ResponseEntity<ApiResponse<DashboardDto>> getDashboard(
            @RequestParam Long scanId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.buildDashboard(scanId)));
    }
}
