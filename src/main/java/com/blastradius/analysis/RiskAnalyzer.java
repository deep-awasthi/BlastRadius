package com.blastradius.analysis;

import com.blastradius.entity.ComponentNode;
import com.blastradius.entity.ComponentNode.RiskCategory;
import com.blastradius.graph.GraphManager;
import com.blastradius.repository.ComponentNodeRepository;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Computes risk scores (0–100) for each component based on:
 *   - Fan-in (number of dependents)
 *   - Fan-out (number of dependencies)
 *   - Participation in circular dependencies
 *   - Dead code status
 */
@Component
public class RiskAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(RiskAnalyzer.class);

    private static final double WEIGHT_FAN_IN = 0.45;
    private static final double WEIGHT_FAN_OUT = 0.25;
    private static final double WEIGHT_CYCLE = 0.30;
    private static final int MAX_FAN_SCORE = 100;

    private final ComponentNodeRepository nodeRepository;
    private final GraphManager graphManager;

    public RiskAnalyzer(ComponentNodeRepository nodeRepository, GraphManager graphManager) {
        this.nodeRepository = nodeRepository;
        this.graphManager = graphManager;
    }

    /**
     * Compute and persist risk scores for all components in a scan.
     */
    @Transactional
    public void analyzeAndPersist(Long scanId) {
        log.info("Running risk analysis for scan {}", scanId);

        Graph<Long, DefaultEdge> graph = graphManager.buildGraph(scanId);
        Set<Long> cycleNodes = graphManager.detectCircularDependencies(scanId);
        List<ComponentNode> nodes = nodeRepository.findByScanId(scanId);

        // Compute max fan-in for normalization
        int maxFanIn = nodes.stream()
                .mapToInt(n -> graphManager.fanIn(graph, n.getId()))
                .max().orElse(1);
        int maxFanOut = nodes.stream()
                .mapToInt(n -> graphManager.fanOut(graph, n.getId()))
                .max().orElse(1);

        for (ComponentNode node : nodes) {
            int fanIn = graphManager.fanIn(graph, node.getId());
            int fanOut = graphManager.fanOut(graph, node.getId());
            boolean inCycle = cycleNodes.contains(node.getId());

            double fanInScore = maxFanIn > 0 ? (double) fanIn / maxFanIn * 100 : 0;
            double fanOutScore = maxFanOut > 0 ? (double) fanOut / maxFanOut * 100 : 0;
            double cycleScore = inCycle ? 100.0 : 0.0;

            double riskScore = Math.min(100.0,
                    WEIGHT_FAN_IN * fanInScore +
                    WEIGHT_FAN_OUT * fanOutScore +
                    WEIGHT_CYCLE * cycleScore);

            node.setRiskScore(Math.round(riskScore * 10.0) / 10.0);
            node.setRiskCategory(categorize(riskScore));
            nodeRepository.save(node);
        }

        log.info("Risk analysis complete for scan {}", scanId);
    }

    private RiskCategory categorize(double score) {
        if (score >= 75) return RiskCategory.CRITICAL;
        if (score >= 50) return RiskCategory.HIGH;
        if (score >= 25) return RiskCategory.MEDIUM;
        return RiskCategory.LOW;
    }
}
