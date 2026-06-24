package com.blastradius.analysis;

import com.blastradius.entity.ComponentNode;
import com.blastradius.entity.ComponentNode.ComponentType;
import com.blastradius.graph.GraphManager;
import com.blastradius.repository.ComponentNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Detects dead code — components with no incoming dependencies
 * that can be considered unused within the scanned codebase.
 *
 * Excludes API controllers, configurations, and scheduled jobs
 * since they are typically entry points.
 */
@Component
public class DeadCodeDetector {

    private static final Logger log = LoggerFactory.getLogger(DeadCodeDetector.class);

    // Entry point types that naturally have no callers
    private static final Set<ComponentType> ENTRY_POINT_TYPES = Set.of(
            ComponentType.API,
            ComponentType.CONFIGURATION,
            ComponentType.JOB,
            ComponentType.EVENT_CONSUMER
    );

    private final ComponentNodeRepository nodeRepository;
    private final GraphManager graphManager;

    public DeadCodeDetector(ComponentNodeRepository nodeRepository, GraphManager graphManager) {
        this.nodeRepository = nodeRepository;
        this.graphManager = graphManager;
    }

    /**
     * Find and mark all dead code candidates in a scan.
     */
    @Transactional
    public List<ComponentNode> detectAndMark(Long scanId) {
        Set<Long> deadCandidates = graphManager.findDeadCodeCandidates(scanId);
        List<ComponentNode> allNodes = nodeRepository.findByScanId(scanId);

        List<ComponentNode> deadNodes = allNodes.stream()
                .filter(n -> deadCandidates.contains(n.getId()))
                .filter(n -> !ENTRY_POINT_TYPES.contains(n.getComponentType()))
                .toList();

        deadNodes.forEach(n -> {
            n.setDeadCode(true);
            nodeRepository.save(n);
        });

        log.info("Dead code detection: {} candidates marked for scan {}", deadNodes.size(), scanId);
        return deadNodes;
    }
}
