package com.blastradius.repository;

import com.blastradius.entity.ComponentNode;
import com.blastradius.entity.ComponentNode.ComponentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComponentNodeRepository extends JpaRepository<ComponentNode, Long> {

    List<ComponentNode> findByScanId(Long scanId);

    List<ComponentNode> findByScanIdAndComponentType(Long scanId, ComponentType componentType);

    @Query("SELECT c FROM ComponentNode c WHERE c.scan.id = :scanId AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(c.qualifiedName) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<ComponentNode> searchByScanId(@Param("scanId") Long scanId, @Param("query") String query);

    @Query("SELECT c FROM ComponentNode c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.qualifiedName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<ComponentNode> searchAll(@Param("query") String query);

    @Query("SELECT c FROM ComponentNode c WHERE c.scan.id = :scanId AND c.deadCode = true")
    List<ComponentNode> findDeadCodeByScanId(@Param("scanId") Long scanId);

    @Query("SELECT COUNT(c) FROM ComponentNode c WHERE c.scan.id = :scanId AND c.componentType = :type")
    long countByScanIdAndType(@Param("scanId") Long scanId, @Param("type") ComponentType type);

    @Query("SELECT c FROM ComponentNode c WHERE c.scan.id = :scanId ORDER BY c.riskScore DESC")
    List<ComponentNode> findByScanIdOrderByRiskScoreDesc(@Param("scanId") Long scanId);

    Optional<ComponentNode> findByScanIdAndQualifiedName(Long scanId, String qualifiedName);

    @Query("SELECT c FROM ComponentNode c WHERE c.scan.id = :scanId AND c.componentType = :type ORDER BY c.riskScore DESC")
    List<ComponentNode> findByScanIdAndTypeOrderByRiskDesc(@Param("scanId") Long scanId, @Param("type") ComponentType type);
}
