package com.blastradius.dto;

import com.blastradius.entity.ComponentNode;
import java.time.LocalDateTime;

public class ComponentNodeDto {

    private Long id;
    private String name;
    private String qualifiedName;
    private String componentType;
    private String filePath;
    private String packageName;
    private Integer linesOfCode;
    private String httpMethod;
    private String endpointPath;
    private String tableName;
    private String metadata;
    private Double riskScore;
    private String riskCategory;
    private Boolean deadCode;
    private Long scanId;
    private LocalDateTime createdAt;

    public static ComponentNodeDto from(ComponentNode node) {
        ComponentNodeDto dto = new ComponentNodeDto();
        dto.id = node.getId();
        dto.name = node.getName();
        dto.qualifiedName = node.getQualifiedName();
        dto.componentType = node.getComponentType() != null ? node.getComponentType().name() : null;
        dto.filePath = node.getFilePath();
        dto.packageName = node.getPackageName();
        dto.linesOfCode = node.getLinesOfCode();
        dto.httpMethod = node.getHttpMethod();
        dto.endpointPath = node.getEndpointPath();
        dto.tableName = node.getTableName();
        dto.metadata = node.getMetadata();
        dto.riskScore = node.getRiskScore();
        dto.riskCategory = node.getRiskCategory() != null ? node.getRiskCategory().name() : null;
        dto.deadCode = node.getDeadCode();
        dto.scanId = node.getScan() != null ? node.getScan().getId() : null;
        dto.createdAt = node.getCreatedAt();
        return dto;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getQualifiedName() { return qualifiedName; }
    public void setQualifiedName(String qualifiedName) { this.qualifiedName = qualifiedName; }
    public String getComponentType() { return componentType; }
    public void setComponentType(String componentType) { this.componentType = componentType; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public Integer getLinesOfCode() { return linesOfCode; }
    public void setLinesOfCode(Integer linesOfCode) { this.linesOfCode = linesOfCode; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getEndpointPath() { return endpointPath; }
    public void setEndpointPath(String endpointPath) { this.endpointPath = endpointPath; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }
    public Boolean getDeadCode() { return deadCode; }
    public void setDeadCode(Boolean deadCode) { this.deadCode = deadCode; }
    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
