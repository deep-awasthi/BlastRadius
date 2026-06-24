package com.blastradius.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a discovered code component node (API, Service, Entity, etc.)
 * in the dependency graph.
 */
@Entity
@Table(name = "component_nodes", indexes = {
    @Index(name = "idx_cn_scan_id", columnList = "scan_id"),
    @Index(name = "idx_cn_type", columnList = "component_type"),
    @Index(name = "idx_cn_name", columnList = "name")
})
public class ComponentNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(name = "qualified_name", length = 1000)
    private String qualifiedName;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 30)
    private ComponentType componentType;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "package_name", length = 500)
    private String packageName;

    @Column(name = "lines_of_code")
    private Integer linesOfCode;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "endpoint_path", length = 500)
    private String endpointPath;

    @Column(name = "table_name", length = 200)
    private String tableName;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "risk_score")
    private Double riskScore = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", length = 20)
    private RiskCategory riskCategory = RiskCategory.LOW;

    @Column(name = "is_dead_code")
    private Boolean deadCode = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id")
    private Scan scan;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum ComponentType {
        API, SERVICE, REPOSITORY, ENTITY, TABLE, DTO, CONFIGURATION, JOB, EVENT, EVENT_PUBLISHER, EVENT_CONSUMER, MODULE, UNKNOWN
    }

    public enum RiskCategory {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    // Constructors
    public ComponentNode() {}

    public ComponentNode(String name, ComponentType componentType, Scan scan) {
        this.name = name;
        this.componentType = componentType;
        this.scan = scan;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getQualifiedName() { return qualifiedName; }
    public void setQualifiedName(String qualifiedName) { this.qualifiedName = qualifiedName; }

    public ComponentType getComponentType() { return componentType; }
    public void setComponentType(ComponentType componentType) { this.componentType = componentType; }

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

    public RiskCategory getRiskCategory() { return riskCategory; }
    public void setRiskCategory(RiskCategory riskCategory) { this.riskCategory = riskCategory; }

    public Boolean getDeadCode() { return deadCode; }
    public void setDeadCode(Boolean deadCode) { this.deadCode = deadCode; }

    public Scan getScan() { return scan; }
    public void setScan(Scan scan) { this.scan = scan; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
