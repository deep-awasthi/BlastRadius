package com.blastradius.dto;

import com.blastradius.entity.ComponentRelationship;
import java.time.LocalDateTime;

public class RelationshipDto {

    private Long id;
    private Long sourceNodeId;
    private String sourceNodeName;
    private String sourceNodeType;
    private Long targetNodeId;
    private String targetNodeName;
    private String targetNodeType;
    private String relationshipType;
    private String description;
    private Long scanId;
    private LocalDateTime createdAt;

    public static RelationshipDto from(ComponentRelationship rel) {
        RelationshipDto dto = new RelationshipDto();
        dto.id = rel.getId();
        if (rel.getSourceNode() != null) {
            dto.sourceNodeId = rel.getSourceNode().getId();
            dto.sourceNodeName = rel.getSourceNode().getName();
            dto.sourceNodeType = rel.getSourceNode().getComponentType() != null
                ? rel.getSourceNode().getComponentType().name() : null;
        }
        if (rel.getTargetNode() != null) {
            dto.targetNodeId = rel.getTargetNode().getId();
            dto.targetNodeName = rel.getTargetNode().getName();
            dto.targetNodeType = rel.getTargetNode().getComponentType() != null
                ? rel.getTargetNode().getComponentType().name() : null;
        }
        dto.relationshipType = rel.getRelationshipType() != null ? rel.getRelationshipType().name() : null;
        dto.description = rel.getDescription();
        dto.scanId = rel.getScan() != null ? rel.getScan().getId() : null;
        dto.createdAt = rel.getCreatedAt();
        return dto;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(Long sourceNodeId) { this.sourceNodeId = sourceNodeId; }
    public String getSourceNodeName() { return sourceNodeName; }
    public void setSourceNodeName(String sourceNodeName) { this.sourceNodeName = sourceNodeName; }
    public String getSourceNodeType() { return sourceNodeType; }
    public void setSourceNodeType(String sourceNodeType) { this.sourceNodeType = sourceNodeType; }
    public Long getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(Long targetNodeId) { this.targetNodeId = targetNodeId; }
    public String getTargetNodeName() { return targetNodeName; }
    public void setTargetNodeName(String targetNodeName) { this.targetNodeName = targetNodeName; }
    public String getTargetNodeType() { return targetNodeType; }
    public void setTargetNodeType(String targetNodeType) { this.targetNodeType = targetNodeType; }
    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
