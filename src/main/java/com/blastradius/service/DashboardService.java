package com.blastradius.service;

import com.blastradius.dto.ComponentNodeDto;
import com.blastradius.dto.DashboardDto;
import com.blastradius.entity.ComponentNode.ComponentType;
import com.blastradius.entity.Scan;
import com.blastradius.exception.ResourceNotFoundException;
import com.blastradius.graph.GraphManager;
import com.blastradius.repository.ComponentNodeRepository;
import com.blastradius.repository.ComponentRelationshipRepository;
import com.blastradius.repository.ScanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the aggregated dashboard metrics for a scan.
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final ScanRepository scanRepository;
    private final ComponentNodeRepository nodeRepository;
    private final ComponentRelationshipRepository relationshipRepository;
    private final GraphManager graphManager;

    public DashboardService(ScanRepository scanRepository,
                             ComponentNodeRepository nodeRepository,
                             ComponentRelationshipRepository relationshipRepository,
                             GraphManager graphManager) {
        this.scanRepository = scanRepository;
        this.nodeRepository = nodeRepository;
        this.relationshipRepository = relationshipRepository;
        this.graphManager = graphManager;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "#scanId")
    public DashboardDto buildDashboard(Long scanId) {
        log.info("Building dashboard for scan {}", scanId);

        Scan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan", scanId));

        DashboardDto dto = new DashboardDto();
        dto.setScanId(scanId);
        dto.setRepoName(scan.getRepoName());

        dto.setTotalApis(nodeRepository.countByScanIdAndType(scanId, ComponentType.API));
        dto.setTotalServices(nodeRepository.countByScanIdAndType(scanId, ComponentType.SERVICE));
        dto.setTotalRepositories(nodeRepository.countByScanIdAndType(scanId, ComponentType.REPOSITORY));
        dto.setTotalEntities(nodeRepository.countByScanIdAndType(scanId, ComponentType.ENTITY));
        dto.setTotalTables(nodeRepository.countByScanIdAndType(scanId, ComponentType.TABLE));
        dto.setTotalEvents(nodeRepository.countByScanIdAndType(scanId, ComponentType.EVENT) +
                           nodeRepository.countByScanIdAndType(scanId, ComponentType.EVENT_CONSUMER) +
                           nodeRepository.countByScanIdAndType(scanId, ComponentType.EVENT_PUBLISHER));
        dto.setTotalConfigurations(nodeRepository.countByScanIdAndType(scanId, ComponentType.CONFIGURATION));
        dto.setTotalJobs(nodeRepository.countByScanIdAndType(scanId, ComponentType.JOB));
        dto.setTotalDependencies(relationshipRepository.countByScanId(scanId));
        dto.setCircularDependencies(graphManager.countCircularDependencies(scanId));

        // Dead code count
        long deadCount = nodeRepository.findDeadCodeByScanId(scanId).size();
        dto.setDeadCodeCount(deadCount);

        // Risk stats
        List<ComponentNodeDto> topRisk = nodeRepository.findByScanIdOrderByRiskScoreDesc(scanId).stream()
                .limit(10)
                .map(ComponentNodeDto::from)
                .collect(Collectors.toList());
        dto.setTopRiskComponents(topRisk);
        dto.setCriticalRiskComponents(topRisk.stream()
                .filter(n -> "CRITICAL".equals(n.getRiskCategory())).count());
        dto.setHighRiskComponents(topRisk.stream()
                .filter(n -> "HIGH".equals(n.getRiskCategory())).count());

        // Most connected (highest incoming deps)
        List<ComponentNodeDto> mostConnected = nodeRepository.findByScanIdOrderByRiskScoreDesc(scanId).stream()
                .limit(5)
                .map(ComponentNodeDto::from)
                .collect(Collectors.toList());
        dto.setMostConnectedComponents(mostConnected);

        // Components by type
        Map<String, Long> byType = new LinkedHashMap<>();
        for (ComponentType type : ComponentType.values()) {
            long count = nodeRepository.countByScanIdAndType(scanId, type);
            if (count > 0) byType.put(type.name(), count);
        }
        dto.setComponentsByType(byType);

        return dto;
    }
}
