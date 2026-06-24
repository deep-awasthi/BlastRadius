package com.blastradius.scanner;

import com.blastradius.entity.ComponentNode;
import com.blastradius.entity.ComponentNode.ComponentType;
import com.blastradius.entity.ComponentRelationship;
import com.blastradius.entity.ComponentRelationship.RelationshipType;
import com.blastradius.entity.Scan;
import com.blastradius.repository.ComponentNodeRepository;
import com.blastradius.repository.ComponentRelationshipRepository;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Core Java source scanner using JavaParser AST analysis.
 * Scans a local repository, discovers components and their relationships.
 */
@Component
public class JavaSourceScanner {

    private static final Logger log = LoggerFactory.getLogger(JavaSourceScanner.class);

    // Annotation markers for component type detection
    private static final Set<String> CONTROLLER_ANNOTATIONS = Set.of(
            "RestController", "Controller", "RequestMapping");
    private static final Set<String> SERVICE_ANNOTATIONS = Set.of(
            "Service", "Component", "Transactional");
    private static final Set<String> REPOSITORY_ANNOTATIONS = Set.of(
            "Repository");
    private static final Set<String> ENTITY_ANNOTATIONS = Set.of(
            "Entity", "MappedSuperclass", "Embeddable");
    private static final Set<String> CONFIG_ANNOTATIONS = Set.of(
            "Configuration", "ConfigurationProperties", "EnableAutoConfiguration");
    private static final Set<String> SCHEDULED_ANNOTATIONS = Set.of(
            "Scheduled", "EnableScheduling");
    private static final Set<String> EVENT_LISTENER_ANNOTATIONS = Set.of(
            "EventListener", "KafkaListener", "RabbitListener", "SqsListener");
    private static final Set<String> EVENT_PUBLISHER_ANNOTATIONS = Set.of(
            "ApplicationEventPublisher");
    private static final Set<String> DTO_PATTERNS = Set.of(
            "Dto", "DTO", "Request", "Response", "Payload", "Command", "View");

    private final ComponentNodeRepository nodeRepository;
    private final ComponentRelationshipRepository relationshipRepository;
    private final JavaParser javaParser;

    public JavaSourceScanner(ComponentNodeRepository nodeRepository,
                             ComponentRelationshipRepository relationshipRepository) {
        this.nodeRepository = nodeRepository;
        this.relationshipRepository = relationshipRepository;
        this.javaParser = new JavaParser();
    }

    /**
     * Main entry point: scan all Java files under the given directory path.
     *
     * @return ScanResult containing counts
     */
    @Transactional
    public ScanResult scan(String repoPath, Scan scan) {
        Path root = Paths.get(repoPath);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            throw new IllegalArgumentException("Repository path does not exist or is not a directory: " + repoPath);
        }

        log.info("Starting scan of repository: {}", repoPath);

        // Phase 1: Parse all Java files and create ComponentNodes
        Map<String, ComponentNode> nodesByQualifiedName = new HashMap<>();
        int fileCount = 0;

        try (Stream<Path> javaFiles = Files.walk(root)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.toString().contains("/.git/"))
                .filter(p -> !p.toString().contains("/target/"))
                .filter(p -> !p.toString().contains("/build/"))) {

            for (Path javaFile : (Iterable<Path>) javaFiles::iterator) {
                try {
                    processJavaFile(javaFile, scan, nodesByQualifiedName);
                    fileCount++;
                } catch (Exception e) {
                    log.warn("Failed to parse file: {} — {}", javaFile, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk repository directory", e);
        }

        // Phase 2: Build relationships from field injection and method call analysis
        int relCount = buildRelationships(root, scan, nodesByQualifiedName);

        log.info("Scan completed: {} files, {} components, {} relationships",
                fileCount, nodesByQualifiedName.size(), relCount);

        return new ScanResult(fileCount, nodesByQualifiedName.size(), relCount);
    }

    private void processJavaFile(Path filePath, Scan scan,
                                  Map<String, ComponentNode> nodesByQualifiedName) throws IOException {
        ParseResult<CompilationUnit> result = javaParser.parse(filePath);
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            log.debug("Could not parse: {}", filePath);
            return;
        }

        CompilationUnit cu = result.getResult().get();
        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("(default)");

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            if (clazz.isInterface() && !isAnnotatedWith(clazz, REPOSITORY_ANNOTATIONS)) {
                return; // skip plain interfaces unless @Repository
            }

            String className = clazz.getNameAsString();
            String qualifiedName = packageName + "." + className;
            ComponentType type = detectComponentType(clazz);

            ComponentNode node = new ComponentNode(className, type, scan);
            node.setQualifiedName(qualifiedName);
            node.setPackageName(packageName);
            node.setFilePath(filePath.toString());
            node.setLinesOfCode(estimateLinesOfCode(cu));

            // Extract HTTP method and endpoint for APIs
            if (type == ComponentType.API) {
                extractApiMetadata(clazz, node);
            }

            // Extract @Table annotation for entities
            if (type == ComponentType.ENTITY) {
                extractEntityMetadata(clazz, node);
            }

            ComponentNode saved = nodeRepository.save(node);
            nodesByQualifiedName.put(qualifiedName, saved);
            log.debug("Discovered {} [{}] in {}", className, type, packageName);
        });
    }

    private ComponentType detectComponentType(ClassOrInterfaceDeclaration clazz) {
        Set<String> annotations = getAnnotationNames(clazz);

        if (hasAnyAnnotation(annotations, CONTROLLER_ANNOTATIONS)) return ComponentType.API;
        if (hasAnyAnnotation(annotations, ENTITY_ANNOTATIONS)) return ComponentType.ENTITY;
        if (hasAnyAnnotation(annotations, REPOSITORY_ANNOTATIONS)) return ComponentType.REPOSITORY;
        if (hasAnyAnnotation(annotations, CONFIG_ANNOTATIONS)) return ComponentType.CONFIGURATION;
        if (hasAnyAnnotation(annotations, SCHEDULED_ANNOTATIONS)) return ComponentType.JOB;
        if (hasAnyAnnotation(annotations, EVENT_LISTENER_ANNOTATIONS)) return ComponentType.EVENT_CONSUMER;
        if (isDto(clazz.getNameAsString())) return ComponentType.DTO;
        if (hasAnyAnnotation(annotations, SERVICE_ANNOTATIONS)) return ComponentType.SERVICE;

        // Check for ApplicationEventPublisher fields
        boolean hasPublisherField = clazz.getFields().stream()
                .anyMatch(f -> f.getVariables().stream()
                        .anyMatch(v -> v.getTypeAsString().contains("ApplicationEventPublisher")));
        if (hasPublisherField) return ComponentType.EVENT_PUBLISHER;

        return ComponentType.SERVICE; // default to SERVICE for non-interface classes
    }

    private boolean isDto(String className) {
        return DTO_PATTERNS.stream().anyMatch(className::contains);
    }

    private boolean hasAnyAnnotation(Set<String> annotations, Set<String> targets) {
        return annotations.stream().anyMatch(targets::contains);
    }

    private boolean isAnnotatedWith(ClassOrInterfaceDeclaration clazz, Set<String> targets) {
        return hasAnyAnnotation(getAnnotationNames(clazz), targets);
    }

    private Set<String> getAnnotationNames(ClassOrInterfaceDeclaration clazz) {
        Set<String> names = new HashSet<>();
        clazz.getAnnotations().forEach(ann -> names.add(ann.getNameAsString()));
        return names;
    }

    private void extractApiMetadata(ClassOrInterfaceDeclaration clazz, ComponentNode node) {
        // Find class-level @RequestMapping
        Optional<AnnotationExpr> classMapping = clazz.getAnnotationByName("RequestMapping");
        String basePath = classMapping.map(this::extractMappingPath).orElse("");

        // Find all method-level mappings
        List<String> endpoints = new ArrayList<>();
        for (MethodDeclaration method : clazz.getMethods()) {
            String path = "";
            String httpMethod = "";

            if (method.getAnnotationByName("GetMapping").isPresent()) {
                httpMethod = "GET";
                path = extractMappingPath(method.getAnnotationByName("GetMapping").get());
            } else if (method.getAnnotationByName("PostMapping").isPresent()) {
                httpMethod = "POST";
                path = extractMappingPath(method.getAnnotationByName("PostMapping").get());
            } else if (method.getAnnotationByName("PutMapping").isPresent()) {
                httpMethod = "PUT";
                path = extractMappingPath(method.getAnnotationByName("PutMapping").get());
            } else if (method.getAnnotationByName("DeleteMapping").isPresent()) {
                httpMethod = "DELETE";
                path = extractMappingPath(method.getAnnotationByName("DeleteMapping").get());
            } else if (method.getAnnotationByName("PatchMapping").isPresent()) {
                httpMethod = "PATCH";
                path = extractMappingPath(method.getAnnotationByName("PatchMapping").get());
            }

            if (!httpMethod.isEmpty()) {
                endpoints.add(httpMethod + " " + basePath + path);
                if (node.getHttpMethod() == null) node.setHttpMethod(httpMethod);
                if (node.getEndpointPath() == null) node.setEndpointPath(basePath + path);
            }
        }

        if (!endpoints.isEmpty()) {
            node.setMetadata(String.join(", ", endpoints));
        }
    }

    private void extractEntityMetadata(ClassOrInterfaceDeclaration clazz, ComponentNode node) {
        clazz.getAnnotationByName("Table").ifPresent(ann -> {
            String tableName = extractAnnotationStringValue(ann, "name");
            if (tableName != null && !tableName.isBlank()) {
                node.setTableName(tableName);
            }
        });
        if (node.getTableName() == null) {
            // Default table name from class name (simple snake_case conversion)
            node.setTableName(toSnakeCase(node.getName()));
        }
    }

    private String extractMappingPath(AnnotationExpr ann) {
        if (ann instanceof NormalAnnotationExpr nae) {
            for (MemberValuePair pair : nae.getPairs()) {
                if (pair.getNameAsString().equals("value") || pair.getNameAsString().equals("path")) {
                    return pair.getValue().toString().replace("\"", "").replace("{", "").replace("}", "");
                }
            }
        }
        // SingleMemberAnnotationExpr
        try {
            var singleMember = ann.asSingleMemberAnnotationExpr();
            return singleMember.getMemberValue().toString().replace("\"", "");
        } catch (Exception e) {
            return "";
        }
    }

    private String extractAnnotationStringValue(AnnotationExpr ann, String memberName) {
        if (ann instanceof NormalAnnotationExpr nae) {
            for (MemberValuePair pair : nae.getPairs()) {
                if (pair.getNameAsString().equals(memberName)) {
                    return pair.getValue().toString().replace("\"", "");
                }
            }
        }
        return null;
    }

    private int buildRelationships(Path root, Scan scan,
                                    Map<String, ComponentNode> nodesByQualifiedName) {
        int relCount = 0;

        // Re-parse files to extract field injections (field type -> dependency relationship)
        try (Stream<Path> javaFiles = Files.walk(root)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.toString().contains("/target/"))
                .filter(p -> !p.toString().contains("/build/"))) {

            for (Path javaFile : (Iterable<Path>) javaFiles::iterator) {
                try {
                    relCount += extractRelationshipsFromFile(javaFile, scan, nodesByQualifiedName);
                } catch (Exception e) {
                    log.debug("Failed to extract relationships from: {}", javaFile);
                }
            }
        } catch (IOException e) {
            log.error("Error walking directory for relationships", e);
        }

        // Add Entity->Table relationships
        for (ComponentNode node : nodesByQualifiedName.values()) {
            if (node.getComponentType() == ComponentType.ENTITY && node.getTableName() != null) {
                // Create a virtual TABLE node
                String tableKey = "table:" + node.getTableName();
                ComponentNode tableNode = nodesByQualifiedName.computeIfAbsent(tableKey, k -> {
                    ComponentNode tn = new ComponentNode(node.getTableName(), ComponentType.TABLE, scan);
                    tn.setQualifiedName(tableKey);
                    tn.setTableName(node.getTableName());
                    return nodeRepository.save(tn);
                });

                if (!relationshipRepository.existsBySourceNodeIdAndTargetNodeIdAndScanId(
                        node.getId(), tableNode.getId(), scan.getId())) {
                    ComponentRelationship rel = new ComponentRelationship(
                            node, tableNode, RelationshipType.OWNS, scan);
                    relationshipRepository.save(rel);
                    relCount++;
                }
            }
        }

        return relCount;
    }

    private int extractRelationshipsFromFile(Path filePath, Scan scan,
                                              Map<String, ComponentNode> nodesByQualifiedName) throws IOException {
        ParseResult<CompilationUnit> result = javaParser.parse(filePath);
        if (!result.isSuccessful() || result.getResult().isEmpty()) return 0;

        CompilationUnit cu = result.getResult().get();
        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString()).orElse("(default)");

        int relCount = 0;
        for (ClassOrInterfaceDeclaration clazz : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (clazz.isInterface()) continue;

            String qualifiedName = packageName + "." + clazz.getNameAsString();
            ComponentNode sourceNode = nodesByQualifiedName.get(qualifiedName);
            if (sourceNode == null) continue;

            // Extract field-injection dependencies
            for (FieldDeclaration field : clazz.getFields()) {
                boolean isInjected = field.getAnnotationByName("Autowired").isPresent()
                        || field.getAnnotationByName("Inject").isPresent()
                        || field.isFinal(); // Constructor injection fields

                if (!isInjected) continue;

                for (VariableDeclarator var : field.getVariables()) {
                    String fieldType = var.getTypeAsString();
                    // Find target node by simple name
                    ComponentNode targetNode = findNodeBySimpleName(fieldType, nodesByQualifiedName);

                    if (targetNode != null && !targetNode.getId().equals(sourceNode.getId())) {
                        RelationshipType relType = determineRelationshipType(
                                sourceNode.getComponentType(), targetNode.getComponentType());

                        if (!relationshipRepository.existsBySourceNodeIdAndTargetNodeIdAndScanId(
                                sourceNode.getId(), targetNode.getId(), scan.getId())) {
                            ComponentRelationship rel = new ComponentRelationship(
                                    sourceNode, targetNode, relType, scan);
                            relationshipRepository.save(rel);
                            relCount++;
                        }
                    }
                }
            }
        }

        return relCount;
    }

    private ComponentNode findNodeBySimpleName(String simpleName, Map<String, ComponentNode> nodes) {
        return nodes.values().stream()
                .filter(n -> n.getName().equals(simpleName) ||
                             (n.getQualifiedName() != null && n.getQualifiedName().endsWith("." + simpleName)))
                .findFirst()
                .orElse(null);
    }

    private RelationshipType determineRelationshipType(ComponentType source, ComponentType target) {
        if (target == ComponentType.REPOSITORY) return RelationshipType.USES;
        if (target == ComponentType.SERVICE) return RelationshipType.CALLS;
        if (target == ComponentType.ENTITY) return RelationshipType.DEPENDS_ON;
        if (target == ComponentType.EVENT) return RelationshipType.CONSUMES;
        if (target == ComponentType.CONFIGURATION) return RelationshipType.DEPENDS_ON;
        return RelationshipType.USES;
    }

    private int estimateLinesOfCode(CompilationUnit cu) {
        return cu.getRange()
                .map(range -> range.end.line)
                .orElse(0);
    }

    private String toSnakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Result value object from a scan operation.
     */
    public record ScanResult(int fileCount, int componentCount, int relationshipCount) {}
}
