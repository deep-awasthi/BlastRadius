package com.blastradius.service;

import com.blastradius.analysis.ArchitectureDriftDetector;
import com.blastradius.analysis.ArchitectureDriftDetector.ArchitectureViolation;
import com.blastradius.dto.ComponentNodeDto;
import com.blastradius.dto.ImpactAnalysisDto;
import com.blastradius.dto.RelationshipDto;
import com.blastradius.entity.ComponentNode;
import com.blastradius.entity.ComponentNode.ComponentType;
import com.blastradius.exception.ResourceNotFoundException;
import com.blastradius.repository.ComponentNodeRepository;
import com.blastradius.repository.ComponentRelationshipRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates dependency, impact, risk, and architecture reports in multiple formats.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ComponentNodeRepository nodeRepository;
    private final ComponentRelationshipRepository relationshipRepository;
    private final DependencyGraphService dependencyGraphService;
    private final ArchitectureDriftDetector driftDetector;

    public ReportService(ComponentNodeRepository nodeRepository,
                         ComponentRelationshipRepository relationshipRepository,
                         DependencyGraphService dependencyGraphService,
                         ArchitectureDriftDetector driftDetector) {
        this.nodeRepository = nodeRepository;
        this.relationshipRepository = relationshipRepository;
        this.dependencyGraphService = dependencyGraphService;
        this.driftDetector = driftDetector;
    }

    // ---- Dependency Report ----

    @Transactional(readOnly = true)
    public List<RelationshipDto> getDependencyReport(Long scanId) {
        return dependencyGraphService.getRelationships(scanId);
    }

    // ---- Risk Report ----

    @Transactional(readOnly = true)
    public List<ComponentNodeDto> getRiskReport(Long scanId) {
        return dependencyGraphService.getTopRiskComponents(scanId, 100);
    }

    // ---- Architecture Report ----

    @Transactional(readOnly = true)
    public List<ArchitectureViolation> getArchitectureReport(Long scanId) {
        return driftDetector.detect(scanId);
    }

    // ---- PDF Export ----

    @Transactional(readOnly = true)
    public byte[] generateRiskReportPdf(Long scanId) {
        List<ComponentNodeDto> nodes = getRiskReport(scanId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Title
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(30, 30, 80));
            doc.add(new Paragraph("BlastRadius — Risk Report", titleFont));
            doc.add(new Paragraph("Scan ID: " + scanId));
            doc.add(new Paragraph(" "));

            // Table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2f, 2f, 1.5f, 2f});

            String[] headers = {"Component", "Type", "Package", "Risk Score", "Category"};
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            for (String header : headers) {
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                        new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(30, 30, 80));
                cell.setPadding(6);
                table.addCell(cell);
            }

            Font bodyFont = new Font(Font.HELVETICA, 9);
            for (ComponentNodeDto node : nodes) {
                table.addCell(new Phrase(node.getName() != null ? node.getName() : "", bodyFont));
                table.addCell(new Phrase(node.getComponentType() != null ? node.getComponentType() : "", bodyFont));
                table.addCell(new Phrase(node.getPackageName() != null ? node.getPackageName() : "", bodyFont));
                table.addCell(new Phrase(node.getRiskScore() != null ? String.valueOf(node.getRiskScore()) : "0", bodyFont));
                table.addCell(new Phrase(node.getRiskCategory() != null ? node.getRiskCategory() : "LOW", bodyFont));
            }

            doc.add(table);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF report: {}", e.getMessage());
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ---- CSV Export ----

    @Transactional(readOnly = true)
    public String generateComponentsCsv(Long scanId) {
        List<ComponentNode> nodes = nodeRepository.findByScanId(scanId);
        StringBuilder sb = new StringBuilder();
        sb.append("id,name,qualifiedName,type,package,filePath,linesOfCode,riskScore,riskCategory,deadCode\n");
        for (ComponentNode n : nodes) {
            sb.append(csv(n.getId())).append(",")
              .append(csv(n.getName())).append(",")
              .append(csv(n.getQualifiedName())).append(",")
              .append(csv(n.getComponentType())).append(",")
              .append(csv(n.getPackageName())).append(",")
              .append(csv(n.getFilePath())).append(",")
              .append(csv(n.getLinesOfCode())).append(",")
              .append(csv(n.getRiskScore())).append(",")
              .append(csv(n.getRiskCategory())).append(",")
              .append(csv(n.getDeadCode())).append("\n");
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String generateRelationshipsCsv(Long scanId) {
        List<RelationshipDto> rels = getDependencyReport(scanId);
        StringBuilder sb = new StringBuilder();
        sb.append("id,sourceId,sourceName,sourceType,targetId,targetName,targetType,relationshipType\n");
        for (RelationshipDto r : rels) {
            sb.append(csv(r.getId())).append(",")
              .append(csv(r.getSourceNodeId())).append(",")
              .append(csv(r.getSourceNodeName())).append(",")
              .append(csv(r.getSourceNodeType())).append(",")
              .append(csv(r.getTargetNodeId())).append(",")
              .append(csv(r.getTargetNodeName())).append(",")
              .append(csv(r.getTargetNodeType())).append(",")
              .append(csv(r.getRelationshipType())).append("\n");
        }
        return sb.toString();
    }

    private String csv(Object val) {
        if (val == null) return "";
        String s = val.toString().replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s + "\"";
        }
        return s;
    }
}
