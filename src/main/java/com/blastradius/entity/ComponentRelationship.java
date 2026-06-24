package com.blastradius.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a directed dependency relationship between two ComponentNodes.
 */
@Entity
@Table(name = "component_relationships", indexes = {
    @Index(name = "idx_cr_scan_id", columnList = "scan_id"),
    @Index(name = "idx_cr_source", columnList = "source_node_id"),
    @Index(name = "idx_cr_target", columnList = "target_node_id")
})
public class ComponentRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_node_id", nullable = false)
    private ComponentNode sourceNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_node_id", nullable = false)
    private ComponentNode targetNode;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 30)
    private RelationshipType relationshipType;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id")
    private Scan scan;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum RelationshipType {
        USES, CALLS, DEPENDS_ON, PRODUCES, CONSUMES, OWNS, EXTENDS, IMPLEMENTS, IMPORTS
    }

    // Constructors
    public ComponentRelationship() {}

    public ComponentRelationship(ComponentNode source, ComponentNode target, RelationshipType type, Scan scan) {
        this.sourceNode = source;
        this.targetNode = target;
        this.relationshipType = type;
        this.scan = scan;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ComponentNode getSourceNode() { return sourceNode; }
    public void setSourceNode(ComponentNode sourceNode) { this.sourceNode = sourceNode; }

    public ComponentNode getTargetNode() { return targetNode; }
    public void setTargetNode(ComponentNode targetNode) { this.targetNode = targetNode; }

    public RelationshipType getRelationshipType() { return relationshipType; }
    public void setRelationshipType(RelationshipType relationshipType) { this.relationshipType = relationshipType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Scan getScan() { return scan; }
    public void setScan(Scan scan) { this.scan = scan; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
