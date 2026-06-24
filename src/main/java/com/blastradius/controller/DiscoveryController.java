package com.blastradius.controller;

import com.blastradius.dto.ApiResponse;
import com.blastradius.dto.ComponentNodeDto;
import com.blastradius.dto.ImpactAnalysisDto;
import com.blastradius.dto.RelationshipDto;
import com.blastradius.service.DependencyGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * REST controller for discovery queries: APIs, services, entities, tables, and dependency graph.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Discovery & Dependencies", description = "Query discovered components and dependency graph")
@SecurityRequirement(name = "bearerAuth")
public class DiscoveryController {

    private final DependencyGraphService graphService;

    public DiscoveryController(DependencyGraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/apis")
    @Operation(summary = "Get all discovered API endpoints for a scan")
    public ResponseEntity<ApiResponse<List<ComponentNodeDto>>> getApis(
            @RequestParam Long scanId) {
        return ResponseEntity.ok(ApiResponse.success(graphService.getComponents(scanId, "API")));
    }

    @GetMapping("/services")
    @Operation(summary = "Get all discovered services for a scan")
    public ResponseEntity<ApiResponse<List<ComponentNodeDto>>> getServices(
            @RequestParam Long scanId) {
        return ResponseEntity.ok(ApiResponse.success(graphService.getComponents(scanId, "SERVICE")));
    }

    @GetMapping("/entities")
    @Operation(summary = "Get all discovered JPA entities for a scan")
    public ResponseEntity<ApiResponse<List<ComponentNodeDto>>> getEntities(
            @RequestParam Long scanId) {
        return ResponseEntity.ok(ApiResponse.success(graphService.getComponents(scanId, "ENTITY")));
    }

    @GetMapping("/tables")
    @Operation(summary = "Get all discovered database tables for a scan")
    public ResponseEntity<ApiResponse<List<ComponentNodeDto>>> getTables(
            @RequestParam Long scanId) {
        return ResponseEntity.ok(ApiResponse.success(graphService.getComponents(scanId, "TABLE")));
    }

    @GetMapping("/components")
    @Operation(summary = "Get all components for a scan, optionally filtered by type")
    public ResponseEntity<ApiResponse<List<ComponentNodeDto>>> getComponents(
            @RequestParam Long scanId,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.success(graphService.getComponents(scanId, type)));
    }

    @GetMapping("/dependencies")
    @Operation(summary = "Get all dependency relationships for a scan")
    public ResponseEntity<ApiResponse<List<RelationshipDto>>> getDependencies(
            @RequestParam Long scanId) {
        return ResponseEntity.ok(ApiResponse.success(graphService.getRelationships(scanId)));
    }

    @GetMapping("/impact-analysis")
    @Operation(summary = "Analyze impact of changing a specific component")
    public ResponseEntity<ApiResponse<ImpactAnalysisDto>> getImpactAnalysis(
            @RequestParam Long scanId,
            @RequestParam @Parameter(description = "Component node ID to analyze") Long componentId) {
        return ResponseEntity.ok(ApiResponse.success(graphService.analyzeImpact(scanId, componentId)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search components by name across all scans or a specific scan")
    public ResponseEntity<ApiResponse<List<ComponentNodeDto>>> search(
            @RequestParam String query,
            @RequestParam(required = false) Long scanId) {
        return ResponseEntity.ok(ApiResponse.success(graphService.search(scanId, query)));
    }

    @GetMapping("/dead-code")
    @Operation(summary = "List dead code (unreferenced components) in a scan")
    public ResponseEntity<ApiResponse<List<ComponentNodeDto>>> getDeadCode(
            @RequestParam Long scanId) {
        return ResponseEntity.ok(ApiResponse.success(graphService.getDeadCode(scanId)));
    }

    @GetMapping("/circular-dependencies")
    @Operation(summary = "Get IDs of all components involved in circular dependencies")
    public ResponseEntity<ApiResponse<Set<Long>>> getCircularDependencies(
            @RequestParam Long scanId) {
        return ResponseEntity.ok(ApiResponse.success(graphService.getCircularDependencies(scanId)));
    }

    @GetMapping("/risk")
    @Operation(summary = "Get components sorted by risk score (highest first)")
    public ResponseEntity<ApiResponse<List<ComponentNodeDto>>> getRisk(
            @RequestParam Long scanId,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(graphService.getTopRiskComponents(scanId, limit)));
    }
}
