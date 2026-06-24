package com.blastradius.graph;

import com.blastradius.entity.ComponentNode;
import com.blastradius.entity.ComponentRelationship;
import com.blastradius.repository.ComponentNodeRepository;
import com.blastradius.repository.ComponentRelationshipRepository;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds and queries a JGraphT directed dependency graph from persisted data.
 * Supports impact analysis, cycle detection, and path traversal.
 */
@Component
public class GraphManager {

    private static final Logger log = LoggerFactory.getLogger(GraphManager.class);

    private final ComponentNodeRepository nodeRepository;
    private final ComponentRelationshipRepository relationshipRepository;

    public GraphManager(ComponentNodeRepository nodeRepository,
                        ComponentRelationshipRepository relationshipRepository) {
        this.nodeRepository = nodeRepository;
        this.relationshipRepository = relationshipRepository;
    }

    /**
     * Build a JGraphT directed graph for the given scan.
     * Vertices = component node IDs; Edges = directed relationships.
     */
    public Graph<Long, DefaultEdge> buildGraph(Long scanId) {
        Graph<Long, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Add all nodes as vertices
        List<ComponentNode> nodes = nodeRepository.findByScanId(scanId);
        nodes.forEach(node -> graph.addVertex(node.getId()));

        // Add edges
        List<ComponentRelationship> relationships = relationshipRepository.findByScanId(scanId);
        for (ComponentRelationship rel : relationships) {
            Long srcId = rel.getSourceNode().getId();
            Long tgtId = rel.getTargetNode().getId();
            if (graph.containsVertex(srcId) && graph.containsVertex(tgtId)) {
                graph.addEdge(srcId, tgtId);
            }
        }

        log.info("Built graph for scan {}: {} vertices, {} edges", scanId, graph.vertexSet().size(), graph.edgeSet().size());
        return graph;
    }

    /**
     * Find all components that depend on the given component (direct + transitive dependents).
     * Traverses the graph in reverse (i.e., find everything that reaches the given node).
     */
    public Set<Long> findAllDependents(Long scanId, Long nodeId) {
        Graph<Long, DefaultEdge> graph = buildGraph(scanId);

        // Build reverse graph to find what depends ON nodeId
        Graph<Long, DefaultEdge> reversed = new DefaultDirectedGraph<>(DefaultEdge.class);
        graph.vertexSet().forEach(reversed::addVertex);
        graph.edgeSet().forEach(edge -> {
            Long src = graph.getEdgeSource(edge);
            Long tgt = graph.getEdgeTarget(edge);
            reversed.addEdge(tgt, src); // Reverse direction
        });

        Set<Long> dependents = new HashSet<>();
        if (!reversed.containsVertex(nodeId)) return dependents;

        BreadthFirstIterator<Long, DefaultEdge> bfs = new BreadthFirstIterator<>(reversed, nodeId);
        while (bfs.hasNext()) {
            Long visited = bfs.next();
            if (!visited.equals(nodeId)) {
                dependents.add(visited);
            }
        }
        return dependents;
    }

    /**
     * Find direct dependents (one hop in the reverse graph).
     */
    public Set<Long> findDirectDependents(Long scanId, Long nodeId) {
        List<ComponentRelationship> incoming = relationshipRepository.findByTargetNodeId(nodeId);
        return incoming.stream()
                .filter(r -> r.getScan().getId().equals(scanId))
                .map(r -> r.getSourceNode().getId())
                .collect(Collectors.toSet());
    }

    /**
     * Detect all nodes that are part of a cycle (circular dependency) in the graph.
     */
    public Set<Long> detectCircularDependencies(Long scanId) {
        Graph<Long, DefaultEdge> graph = buildGraph(scanId);
        CycleDetector<Long, DefaultEdge> detector = new CycleDetector<>(graph);
        return detector.findCycles();
    }

    /**
     * Get the count of cycles (SCC with more than 1 node).
     */
    public long countCircularDependencies(Long scanId) {
        Set<Long> cycleNodes = detectCircularDependencies(scanId);
        return cycleNodes.size();
    }

    /**
     * Calculate fan-in (incoming edges) for a node.
     */
    public int fanIn(Graph<Long, DefaultEdge> graph, Long nodeId) {
        if (!graph.containsVertex(nodeId)) return 0;
        return graph.inDegreeOf(nodeId);
    }

    /**
     * Calculate fan-out (outgoing edges) for a node.
     */
    public int fanOut(Graph<Long, DefaultEdge> graph, Long nodeId) {
        if (!graph.containsVertex(nodeId)) return 0;
        return graph.outDegreeOf(nodeId);
    }

    /**
     * Detect nodes with no incoming edges in a scan (potential dead code).
     */
    public Set<Long> findDeadCodeCandidates(Long scanId) {
        Graph<Long, DefaultEdge> graph = buildGraph(scanId);
        return graph.vertexSet().stream()
                .filter(v -> graph.inDegreeOf(v) == 0)
                .collect(Collectors.toSet());
    }

    /**
     * Find shortest path between two nodes.
     */
    public List<Long> findShortestPath(Long scanId, Long sourceId, Long targetId) {
        Graph<Long, DefaultEdge> graph = buildGraph(scanId);
        if (!graph.containsVertex(sourceId) || !graph.containsVertex(targetId)) {
            return Collections.emptyList();
        }

        BFSShortestPath<Long, DefaultEdge> bfs = new BFSShortestPath<>(graph);
        var path = bfs.getPath(sourceId, targetId);
        if (path == null) return Collections.emptyList();
        return path.getVertexList();
    }
}
