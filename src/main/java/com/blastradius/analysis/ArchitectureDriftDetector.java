package com.blastradius.analysis;

import com.blastradius.entity.ComponentNode;
import com.blastradius.entity.ComponentNode.ComponentType;
import com.blastradius.entity.ComponentRelationship;
import com.blastradius.repository.ComponentNodeRepository;
import com.blastradius.repository.ComponentRelationshipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Detects architecture violations such as:
 *  - Controller -> Repository direct access (bypassing Service layer)
 *  - Circular Service dependencies
 *  - DTO accessing Service/Repository directly
 */
@Component
public class ArchitectureDriftDetector {

    private static final Logger log = LoggerFactory.getLogger(ArchitectureDriftDetector.class);

    private final ComponentNodeRepository nodeRepository;
    private final ComponentRelationshipRepository relationshipRepository;

    public ArchitectureDriftDetector(ComponentNodeRepository nodeRepository,
                                     ComponentRelationshipRepository relationshipRepository) {
        this.nodeRepository = nodeRepository;
        this.relationshipRepository = relationshipRepository;
    }

    /**
     * Detect all architectural drift violations for a scan.
     * @return list of human-readable violation descriptions
     */
    public List<ArchitectureViolation> detect(Long scanId) {
        List<ComponentRelationship> allRelationships = relationshipRepository.findByScanId(scanId);
        List<ComponentNode> allNodes = nodeRepository.findByScanId(scanId);

        Map<Long, ComponentNode> nodeMap = allNodes.stream()
                .collect(Collectors.toMap(ComponentNode::getId, Function.identity()));

        List<ArchitectureViolation> violations = new ArrayList<>();

        for (ComponentRelationship rel : allRelationships) {
            ComponentNode source = nodeMap.get(rel.getSourceNode().getId());
            ComponentNode target = nodeMap.get(rel.getTargetNode().getId());

            if (source == null || target == null) continue;

            ComponentType srcType = source.getComponentType();
            ComponentType tgtType = target.getComponentType();

            // Rule 1: Controller must NOT directly access Repository
            if (srcType == ComponentType.API && tgtType == ComponentType.REPOSITORY) {
                violations.add(new ArchitectureViolation(
                        ViolationType.CONTROLLER_REPOSITORY_DIRECT_ACCESS,
                        source.getName(),
                        target.getName(),
                        "Controller '" + source.getName() + "' directly accesses Repository '" +
                        target.getName() + "' without going through a Service layer."
                ));
            }

            // Rule 2: DTO must NOT access Service or Repository directly
            if (srcType == ComponentType.DTO &&
                    (tgtType == ComponentType.SERVICE || tgtType == ComponentType.REPOSITORY)) {
                violations.add(new ArchitectureViolation(
                        ViolationType.DTO_LAYER_BYPASS,
                        source.getName(),
                        target.getName(),
                        "DTO '" + source.getName() + "' is referencing '" +
                        target.getName() + "' of type " + tgtType + " — DTOs should be plain data objects."
                ));
            }

            // Rule 3: Repository must NOT call Service (upward dependency)
            if (srcType == ComponentType.REPOSITORY && tgtType == ComponentType.SERVICE) {
                violations.add(new ArchitectureViolation(
                        ViolationType.REPOSITORY_CALLS_SERVICE,
                        source.getName(),
                        target.getName(),
                        "Repository '" + source.getName() + "' depends on Service '" +
                        target.getName() + "' — this is an upward layer violation."
                ));
            }
        }

        log.info("Architecture drift detection found {} violations for scan {}", violations.size(), scanId);
        return violations;
    }

    public enum ViolationType {
        CONTROLLER_REPOSITORY_DIRECT_ACCESS,
        DTO_LAYER_BYPASS,
        REPOSITORY_CALLS_SERVICE,
        CIRCULAR_SERVICE_DEPENDENCY
    }

    public record ArchitectureViolation(
            ViolationType type,
            String sourceComponent,
            String targetComponent,
            String description
    ) {}
}
