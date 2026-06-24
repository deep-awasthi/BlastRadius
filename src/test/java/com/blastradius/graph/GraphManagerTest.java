package com.blastradius.graph;

import com.blastradius.entity.ComponentNode;
import com.blastradius.entity.ComponentNode.ComponentType;
import com.blastradius.entity.ComponentRelationship;
import com.blastradius.entity.ComponentRelationship.RelationshipType;
import com.blastradius.entity.Scan;
import com.blastradius.repository.ComponentNodeRepository;
import com.blastradius.repository.ComponentRelationshipRepository;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GraphManager cycle detection and dependent traversal.
 * Uses mocked repositories injected into a real GraphManager instance.
 */
@ExtendWith(MockitoExtension.class)
class GraphManagerTest {

    @Mock private ComponentNodeRepository nodeRepository;
    @Mock private ComponentRelationshipRepository relationshipRepository;

    private GraphManager graphManager;

    private Scan scan;
    private ComponentNode a, b, c;

    @BeforeEach
    void setUp() {
        graphManager = new GraphManager(nodeRepository, relationshipRepository);
        scan = new Scan("/repo");
        a = node(1L, "ServiceA", ComponentType.SERVICE);
        b = node(2L, "ServiceB", ComponentType.SERVICE);
        c = node(3L, "RepositoryC", ComponentType.REPOSITORY);
    }

    private ComponentNode node(Long id, String name, ComponentType type) {
        ComponentNode n = new ComponentNode(name, type, scan);
        n.setId(id);
        return n;
    }

    private ComponentRelationship rel(ComponentNode src, ComponentNode tgt) {
        ComponentRelationship r = new ComponentRelationship(src, tgt, RelationshipType.USES, scan);
        r.setId(src.getId() * 10 + tgt.getId());
        return r;
    }

    @Test
    void buildGraph_correctVerticesAndEdges() {
        when(nodeRepository.findByScanId(1L)).thenReturn(List.of(a, b, c));
        when(relationshipRepository.findByScanId(1L)).thenReturn(List.of(rel(a, b), rel(b, c)));

        Graph<Long, DefaultEdge> graph = graphManager.buildGraph(1L);

        assertEquals(3, graph.vertexSet().size());
        assertEquals(2, graph.edgeSet().size());
        assertTrue(graph.containsEdge(1L, 2L));
        assertTrue(graph.containsEdge(2L, 3L));
    }

    @Test
    void detectCircularDependencies_withCycle_returnsCycleNodes() {
        // A -> B -> C -> A
        when(nodeRepository.findByScanId(1L)).thenReturn(List.of(a, b, c));
        when(relationshipRepository.findByScanId(1L)).thenReturn(
                List.of(rel(a, b), rel(b, c), rel(c, a)));

        Set<Long> cycles = graphManager.detectCircularDependencies(1L);

        assertFalse(cycles.isEmpty(), "Should detect cycle");
        assertTrue(cycles.containsAll(Set.of(1L, 2L, 3L)));
    }

    @Test
    void detectCircularDependencies_noCycle_returnsEmpty() {
        // A -> B -> C (DAG)
        when(nodeRepository.findByScanId(1L)).thenReturn(List.of(a, b, c));
        when(relationshipRepository.findByScanId(1L)).thenReturn(
                List.of(rel(a, b), rel(b, c)));

        Set<Long> cycles = graphManager.detectCircularDependencies(1L);

        assertTrue(cycles.isEmpty(), "Should not detect cycle in DAG");
    }

    @Test
    void findAllDependents_returnsTransitiveDependents() {
        // B depends on A, C depends on B  →  A is depended on by B and C
        when(nodeRepository.findByScanId(1L)).thenReturn(List.of(a, b, c));
        when(relationshipRepository.findByScanId(1L)).thenReturn(
                List.of(rel(b, a), rel(c, b)));

        Set<Long> dependents = graphManager.findAllDependents(1L, 1L);

        assertTrue(dependents.contains(2L), "B directly depends on A");
        assertTrue(dependents.contains(3L), "C transitively depends on A");
        assertFalse(dependents.contains(1L), "Root A should not appear");
    }

    @Test
    void findDeadCodeCandidates_nodesWithZeroInDegree() {
        // Only edge is A->B; so C has no incoming, A has no incoming
        when(nodeRepository.findByScanId(1L)).thenReturn(List.of(a, b, c));
        when(relationshipRepository.findByScanId(1L)).thenReturn(List.of(rel(a, b)));

        Set<Long> dead = graphManager.findDeadCodeCandidates(1L);

        // A and C have no incoming edges
        assertTrue(dead.contains(1L) || dead.contains(3L),
                "Nodes with no incoming edges should be candidates");
        assertFalse(dead.contains(2L), "B has an incoming edge from A");
    }

    @Test
    void fanIn_correctCount() {
        when(nodeRepository.findByScanId(1L)).thenReturn(List.of(a, b, c));
        when(relationshipRepository.findByScanId(1L)).thenReturn(
                List.of(rel(a, b), rel(c, b))); // B has 2 incoming

        Graph<Long, DefaultEdge> graph = graphManager.buildGraph(1L);

        assertEquals(2, graphManager.fanIn(graph, 2L));
        assertEquals(0, graphManager.fanIn(graph, 1L));
    }
}
