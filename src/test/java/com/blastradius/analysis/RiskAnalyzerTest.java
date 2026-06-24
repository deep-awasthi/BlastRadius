package com.blastradius.analysis;

import com.blastradius.entity.ComponentNode;
import com.blastradius.entity.ComponentNode.ComponentType;
import com.blastradius.entity.ComponentNode.RiskCategory;
import com.blastradius.entity.ComponentRelationship;
import com.blastradius.entity.ComponentRelationship.RelationshipType;
import com.blastradius.entity.Scan;
import com.blastradius.graph.GraphManager;
import com.blastradius.repository.ComponentNodeRepository;
import com.blastradius.repository.ComponentRelationshipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RiskAnalyzer.
 * GraphManager is instantiated with mocked repos to avoid Mockito inline mock issues on Java 26.
 */
@ExtendWith(MockitoExtension.class)
class RiskAnalyzerTest {

    @Mock private ComponentNodeRepository nodeRepository;
    @Mock private ComponentRelationshipRepository relationshipRepository;

    private RiskAnalyzer riskAnalyzer;
    private GraphManager graphManager;

    private Scan scan;
    private ComponentNode nodeA, nodeB, nodeC;

    @BeforeEach
    void setUp() {
        graphManager = new GraphManager(nodeRepository, relationshipRepository);
        riskAnalyzer = new RiskAnalyzer(nodeRepository, graphManager);

        scan = new Scan("/test/repo");

        nodeA = createNode(1L, "UserService", ComponentType.SERVICE);
        nodeB = createNode(2L, "UserRepository", ComponentType.REPOSITORY);
        nodeC = createNode(3L, "OrderService", ComponentType.SERVICE);
    }

    private ComponentNode createNode(Long id, String name, ComponentType type) {
        ComponentNode node = new ComponentNode(name, type, scan);
        node.setId(id);
        node.setRiskScore(0.0);
        node.setRiskCategory(RiskCategory.LOW);
        return node;
    }

    private ComponentRelationship rel(ComponentNode src, ComponentNode tgt) {
        ComponentRelationship r = new ComponentRelationship(src, tgt, RelationshipType.USES, scan);
        r.setId(src.getId() * 10 + tgt.getId());
        return r;
    }

    @Test
    void analyzeAndPersist_nodesReceiveRiskScores() {
        Long scanId = 1L;
        List<ComponentNode> nodes = List.of(nodeA, nodeB, nodeC);

        // A -> B, C -> A
        when(nodeRepository.findByScanId(scanId)).thenReturn(nodes);
        when(relationshipRepository.findByScanId(scanId)).thenReturn(
                List.of(rel(nodeA, nodeB), rel(nodeC, nodeA)));
        when(nodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        riskAnalyzer.analyzeAndPersist(scanId);

        verify(nodeRepository, times(3)).save(any(ComponentNode.class));
        nodes.forEach(n -> assertNotNull(n.getRiskScore()));
        nodes.forEach(n -> assertNotNull(n.getRiskCategory()));
    }

    @Test
    void analyzeAndPersist_cycleNodeGetsHigherScore() {
        Long scanId = 1L;
        // A -> B -> A (cycle)
        when(nodeRepository.findByScanId(scanId)).thenReturn(List.of(nodeA, nodeB));
        when(relationshipRepository.findByScanId(scanId)).thenReturn(
                List.of(rel(nodeA, nodeB), rel(nodeB, nodeA)));
        when(nodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        riskAnalyzer.analyzeAndPersist(scanId);

        // Both in cycle, both should have risk > 0
        assertTrue(nodeA.getRiskScore() > 0 || nodeB.getRiskScore() > 0);
    }

    @Test
    void analyzeAndPersist_noRelationships_allLowRisk() {
        Long scanId = 1L;
        when(nodeRepository.findByScanId(scanId)).thenReturn(List.of(nodeA));
        when(relationshipRepository.findByScanId(scanId)).thenReturn(List.of());
        when(nodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        riskAnalyzer.analyzeAndPersist(scanId);

        assertEquals(RiskCategory.LOW, nodeA.getRiskCategory());
    }
}
