package com.blastradius.service;

import com.blastradius.dto.ComponentNodeDto;
import com.blastradius.dto.ImpactAnalysisDto;
import com.blastradius.dto.RelationshipDto;
import com.blastradius.entity.ComponentNode;
import com.blastradius.entity.ComponentNode.ComponentType;
import com.blastradius.exception.ResourceNotFoundException;
import com.blastradius.graph.GraphManager;
import com.blastradius.repository.ComponentNodeRepository;
import com.blastradius.repository.ComponentRelationshipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for querying the dependency graph, running impact analysis,
 * and searching components.
 */
@Service
public class DependencyGraphService {

    private static final Logger log = LoggerFactory.getLogger(DependencyGraphService.class);

    private final ComponentNodeRepository nodeRepository;
    private final ComponentRelationshipRepository relationshipRepository;
    private final GraphManager graphManager;

    public DependencyGraphService(ComponentNodeRepository nodeRepository,
                                   ComponentRelationshipRepository relationshipRepository,
                                   GraphManager graphManager) {
        this.nodeRepository = nodeRepository;
        this.relationshipRepository = relationshipRepository;
        this.graphManager = graphManager;
    }

    @Transactional(readOnly = true)
    public List<ComponentNodeDto> getComponents(Long scanId, String type) {
        List<ComponentNode> nodes;
        if (type != null && !type.isBlank()) {
            try {
                ComponentType ct = ComponentType.valueOf(type.toUpperCase());
                nodes = nodeRepository.findByScanIdAndComponentType(scanId, ct);
            } catch (IllegalArgumentException e) {
                nodes = nodeRepository.findByScanId(scanId);
            }
        } else {
            nodes = nodeRepository.findByScanId(scanId);
        }
        return nodes.stream().map(ComponentNodeDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RelationshipDto> getRelationships(Long scanId) {
        return relationshipRepository.findByScanId(scanId).stream()
                .map(RelationshipDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "impact-analysis", key = "#scanId + '-' + #componentId")
    public ImpactAnalysisDto analyzeImpact(Long scanId, Long componentId) {
        ComponentNode root = nodeRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("ComponentNode", componentId));

        Set<Long> directDependentIds = graphManager.findDirectDependents(scanId, componentId);
        Set<Long> allDependentIds = graphManager.findAllDependents(scanId, componentId);

        List<ComponentNode> directDependents = nodeRepository.findAllById(directDependentIds);
        List<ComponentNode> allDependents = nodeRepository.findAllById(allDependentIds);

        double overallRisk = allDependents.stream()
                .mapToDouble(n -> n.getRiskScore() != null ? n.getRiskScore() : 0.0)
                .average().orElse(0.0);

        List<String> warnings = new ArrayList<>();
        Set<Long> cycleNodes = graphManager.detectCircularDependencies(scanId);
        if (cycleNodes.contains(componentId)) {
            warnings.add("WARNING: '" + root.getName() + "' is part of a circular dependency chain.");
        }
        if (allDependents.size() > 20) {
            warnings.add("HIGH BLAST RADIUS: This component has " + allDependents.size() +
                         " transitive dependents. Changes carry significant risk.");
        }

        ImpactAnalysisDto dto = new ImpactAnalysisDto();
        dto.setRootComponentId(componentId);
        dto.setRootComponentName(root.getName());
        dto.setRootComponentType(root.getComponentType() != null ? root.getComponentType().name() : null);
        dto.setTotalAffectedComponents(allDependents.size());
        dto.setOverallRiskScore(Math.round(overallRisk * 10.0) / 10.0);
        dto.setRiskCategory(categorize(overallRisk));
        dto.setDirectDependents(directDependents.stream().map(ComponentNodeDto::from).collect(Collectors.toList()));
        dto.setAllAffectedComponents(allDependents.stream().map(ComponentNodeDto::from).collect(Collectors.toList()));
        dto.setWarnings(warnings);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<ComponentNodeDto> search(Long scanId, String query) {
        List<ComponentNode> results = (scanId != null)
                ? nodeRepository.searchByScanId(scanId, query)
                : nodeRepository.searchAll(query);
        return results.stream().map(ComponentNodeDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ComponentNodeDto> getDeadCode(Long scanId) {
        return nodeRepository.findDeadCodeByScanId(scanId).stream()
                .map(ComponentNodeDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ComponentNodeDto> getTopRiskComponents(Long scanId, int limit) {
        return nodeRepository.findByScanIdOrderByRiskScoreDesc(scanId).stream()
                .limit(limit)
                .map(ComponentNodeDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Set<Long> getCircularDependencies(Long scanId) {
        return graphManager.detectCircularDependencies(scanId);
    }

    private String categorize(double score) {
        if (score >= 75) return "CRITICAL";
        if (score >= 50) return "HIGH";
        if (score >= 25) return "MEDIUM";
        return "LOW";
    }
}
