package com.blastradius.dto;

import java.util.List;
import java.util.Set;

public class ImpactAnalysisDto {

    private Long rootComponentId;
    private String rootComponentName;
    private String rootComponentType;
    private int totalAffectedComponents;
    private double overallRiskScore;
    private String riskCategory;
    private List<ComponentNodeDto> directDependents;
    private List<ComponentNodeDto> allAffectedComponents;
    private List<String> impactPath;
    private List<String> warnings;

    public ImpactAnalysisDto() {}

    // Getters & Setters
    public Long getRootComponentId() { return rootComponentId; }
    public void setRootComponentId(Long rootComponentId) { this.rootComponentId = rootComponentId; }
    public String getRootComponentName() { return rootComponentName; }
    public void setRootComponentName(String rootComponentName) { this.rootComponentName = rootComponentName; }
    public String getRootComponentType() { return rootComponentType; }
    public void setRootComponentType(String rootComponentType) { this.rootComponentType = rootComponentType; }
    public int getTotalAffectedComponents() { return totalAffectedComponents; }
    public void setTotalAffectedComponents(int totalAffectedComponents) { this.totalAffectedComponents = totalAffectedComponents; }
    public double getOverallRiskScore() { return overallRiskScore; }
    public void setOverallRiskScore(double overallRiskScore) { this.overallRiskScore = overallRiskScore; }
    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }
    public List<ComponentNodeDto> getDirectDependents() { return directDependents; }
    public void setDirectDependents(List<ComponentNodeDto> directDependents) { this.directDependents = directDependents; }
    public List<ComponentNodeDto> getAllAffectedComponents() { return allAffectedComponents; }
    public void setAllAffectedComponents(List<ComponentNodeDto> allAffectedComponents) { this.allAffectedComponents = allAffectedComponents; }
    public List<String> getImpactPath() { return impactPath; }
    public void setImpactPath(List<String> impactPath) { this.impactPath = impactPath; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
