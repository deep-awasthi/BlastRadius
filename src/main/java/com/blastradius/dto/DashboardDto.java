package com.blastradius.dto;

import java.util.List;
import java.util.Map;

public class DashboardDto {

    private Long scanId;
    private String repoName;
    private long totalApis;
    private long totalServices;
    private long totalRepositories;
    private long totalEntities;
    private long totalTables;
    private long totalEvents;
    private long totalConfigurations;
    private long totalJobs;
    private long totalDependencies;
    private long circularDependencies;
    private long deadCodeCount;
    private long criticalRiskComponents;
    private long highRiskComponents;
    private List<ComponentNodeDto> topRiskComponents;
    private List<ComponentNodeDto> mostConnectedComponents;
    private Map<String, Long> componentsByType;

    public DashboardDto() {}

    // Getters & Setters
    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }
    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }
    public long getTotalApis() { return totalApis; }
    public void setTotalApis(long totalApis) { this.totalApis = totalApis; }
    public long getTotalServices() { return totalServices; }
    public void setTotalServices(long totalServices) { this.totalServices = totalServices; }
    public long getTotalRepositories() { return totalRepositories; }
    public void setTotalRepositories(long totalRepositories) { this.totalRepositories = totalRepositories; }
    public long getTotalEntities() { return totalEntities; }
    public void setTotalEntities(long totalEntities) { this.totalEntities = totalEntities; }
    public long getTotalTables() { return totalTables; }
    public void setTotalTables(long totalTables) { this.totalTables = totalTables; }
    public long getTotalEvents() { return totalEvents; }
    public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }
    public long getTotalConfigurations() { return totalConfigurations; }
    public void setTotalConfigurations(long totalConfigurations) { this.totalConfigurations = totalConfigurations; }
    public long getTotalJobs() { return totalJobs; }
    public void setTotalJobs(long totalJobs) { this.totalJobs = totalJobs; }
    public long getTotalDependencies() { return totalDependencies; }
    public void setTotalDependencies(long totalDependencies) { this.totalDependencies = totalDependencies; }
    public long getCircularDependencies() { return circularDependencies; }
    public void setCircularDependencies(long circularDependencies) { this.circularDependencies = circularDependencies; }
    public long getDeadCodeCount() { return deadCodeCount; }
    public void setDeadCodeCount(long deadCodeCount) { this.deadCodeCount = deadCodeCount; }
    public long getCriticalRiskComponents() { return criticalRiskComponents; }
    public void setCriticalRiskComponents(long criticalRiskComponents) { this.criticalRiskComponents = criticalRiskComponents; }
    public long getHighRiskComponents() { return highRiskComponents; }
    public void setHighRiskComponents(long highRiskComponents) { this.highRiskComponents = highRiskComponents; }
    public List<ComponentNodeDto> getTopRiskComponents() { return topRiskComponents; }
    public void setTopRiskComponents(List<ComponentNodeDto> topRiskComponents) { this.topRiskComponents = topRiskComponents; }
    public List<ComponentNodeDto> getMostConnectedComponents() { return mostConnectedComponents; }
    public void setMostConnectedComponents(List<ComponentNodeDto> mostConnectedComponents) { this.mostConnectedComponents = mostConnectedComponents; }
    public Map<String, Long> getComponentsByType() { return componentsByType; }
    public void setComponentsByType(Map<String, Long> componentsByType) { this.componentsByType = componentsByType; }
}
