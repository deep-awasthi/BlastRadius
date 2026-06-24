package com.blastradius.repository;

import com.blastradius.entity.ComponentRelationship;
import com.blastradius.entity.ComponentRelationship.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComponentRelationshipRepository extends JpaRepository<ComponentRelationship, Long> {

    List<ComponentRelationship> findByScanId(Long scanId);

    List<ComponentRelationship> findBySourceNodeId(Long sourceNodeId);

    List<ComponentRelationship> findByTargetNodeId(Long targetNodeId);

    List<ComponentRelationship> findByScanIdAndRelationshipType(Long scanId, RelationshipType type);

    @Query("SELECT r FROM ComponentRelationship r WHERE r.scan.id = :scanId AND r.sourceNode.id = :nodeId")
    List<ComponentRelationship> findOutgoingRelationships(@Param("scanId") Long scanId, @Param("nodeId") Long nodeId);

    @Query("SELECT r FROM ComponentRelationship r WHERE r.scan.id = :scanId AND r.targetNode.id = :nodeId")
    List<ComponentRelationship> findIncomingRelationships(@Param("scanId") Long scanId, @Param("nodeId") Long nodeId);

    @Query("SELECT COUNT(r) FROM ComponentRelationship r WHERE r.scan.id = :scanId")
    long countByScanId(@Param("scanId") Long scanId);

    @Query("SELECT r.targetNode.id, COUNT(r) as inCount FROM ComponentRelationship r WHERE r.scan.id = :scanId " +
           "GROUP BY r.targetNode.id ORDER BY inCount DESC")
    List<Object[]> findMostConnectedTargets(@Param("scanId") Long scanId);

    boolean existsBySourceNodeIdAndTargetNodeIdAndScanId(Long sourceId, Long targetId, Long scanId);
}
