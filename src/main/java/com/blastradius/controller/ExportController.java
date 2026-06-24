package com.blastradius.controller;

import com.blastradius.dto.ApiResponse;
import com.blastradius.dto.ComponentNodeDto;
import com.blastradius.dto.RelationshipDto;
import com.blastradius.service.DependencyGraphService;
import com.blastradius.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for data exports: JSON, CSV, PDF.
 */
@RestController
@RequestMapping("/api/export")
@Tag(name = "Export", description = "Export dependency graph and reports in JSON, CSV, PDF formats")
@SecurityRequirement(name = "bearerAuth")
public class ExportController {

    private final DependencyGraphService graphService;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    public ExportController(DependencyGraphService graphService,
                            ReportService reportService,
                            ObjectMapper objectMapper) {
        this.graphService = graphService;
        this.reportService = reportService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/json")
    @Operation(summary = "Export full dependency graph as JSON")
    public ResponseEntity<byte[]> exportJson(@RequestParam Long scanId) throws Exception {
        List<ComponentNodeDto> nodes = graphService.getComponents(scanId, null);
        List<RelationshipDto> edges = graphService.getRelationships(scanId);

        Map<String, Object> graph = Map.of(
                "scanId", scanId,
                "nodes", nodes,
                "edges", edges,
                "nodeCount", nodes.size(),
                "edgeCount", edges.size()
        );

        byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(graph);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"blastradius-graph-scan-" + scanId + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @GetMapping("/csv/components")
    @Operation(summary = "Export all components as CSV")
    public ResponseEntity<byte[]> exportComponentsCsv(@RequestParam Long scanId) {
        String csv = reportService.generateComponentsCsv(scanId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"blastradius-components-scan-" + scanId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/csv/relationships")
    @Operation(summary = "Export all dependency relationships as CSV")
    public ResponseEntity<byte[]> exportRelationshipsCsv(@RequestParam Long scanId) {
        String csv = reportService.generateRelationshipsCsv(scanId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"blastradius-relationships-scan-" + scanId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/csv")
    @Operation(summary = "Export components CSV (alias)")
    public ResponseEntity<byte[]> exportCsv(@RequestParam Long scanId) {
        return exportComponentsCsv(scanId);
    }

    @GetMapping("/pdf/risk")
    @Operation(summary = "Export risk report as PDF")
    public ResponseEntity<byte[]> exportRiskPdf(@RequestParam Long scanId) {
        byte[] pdf = reportService.generateRiskReportPdf(scanId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"blastradius-risk-report-scan-" + scanId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
