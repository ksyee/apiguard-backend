package com.apiguard.backend.domain.apispec.service;

import com.apiguard.backend.domain.apispec.dto.ApiSpecDiffDetailResponse;
import com.apiguard.backend.domain.apispec.dto.ApiSpecDiffResponse;
import com.apiguard.backend.domain.apispec.dto.ApiSpecSourceResponse;
import com.apiguard.backend.domain.apispec.dto.BreakingChangeResponse;
import com.apiguard.backend.domain.apispec.dto.CreateApiSpecSourceRequest;
import com.apiguard.backend.domain.apispec.dto.UpdateApiSpecSourceRequest;
import com.apiguard.backend.domain.apispec.entity.ApiSpecDiff;
import com.apiguard.backend.domain.apispec.entity.ApiSpecSnapshot;
import com.apiguard.backend.domain.apispec.entity.ApiSpecSource;
import com.apiguard.backend.domain.apispec.entity.BreakingChange;
import com.apiguard.backend.domain.apispec.entity.BreakingChangeRule;
import com.apiguard.backend.domain.apispec.repository.ApiSpecDiffRepository;
import com.apiguard.backend.domain.apispec.repository.ApiSpecSnapshotRepository;
import com.apiguard.backend.domain.apispec.repository.ApiSpecSourceRepository;
import com.apiguard.backend.domain.apispec.repository.BreakingChangeRepository;
import com.apiguard.backend.domain.incident.service.IncidentService;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.service.ProjectService;
import com.apiguard.backend.global.exception.ApiSpecNotFoundException;
import com.apiguard.backend.global.security.OutboundUrlGuard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiSpecService {

    private static final Set<String> HTTP_METHODS = Set.of(
        "get", "post", "put", "patch", "delete", "head", "options", "trace"
    );

    private final ApiSpecSourceRepository specSourceRepository;
    private final ApiSpecSnapshotRepository specSnapshotRepository;
    private final ApiSpecDiffRepository specDiffRepository;
    private final BreakingChangeRepository breakingChangeRepository;
    private final ProjectService projectService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final IncidentService incidentService;
    private final OutboundUrlGuard outboundUrlGuard;

    @Transactional
    public ApiSpecSourceResponse createSource(Long projectId, CreateApiSpecSourceRequest request) {
        Project project = projectService.getProjectWithMemberCheck(projectId);

        ApiSpecSource source = ApiSpecSource.builder()
            .project(project)
            .name(request.name())
            .specUrl(request.specUrl())
            .build();

        return ApiSpecSourceResponse.from(specSourceRepository.save(source));
    }

    public List<ApiSpecSourceResponse> getSources(Long projectId) {
        projectService.getProjectWithAccessCheck(projectId);
        return specSourceRepository.findByProjectIdAndDeletedFalseOrderByCreatedAtDesc(projectId).stream()
            .map(ApiSpecSourceResponse::from)
            .toList();
    }

    @Transactional
    public ApiSpecSourceResponse updateSource(Long sourceId, UpdateApiSpecSourceRequest request) {
        ApiSpecSource source = getSourceWithWriteCheck(sourceId);
        source.update(
            normalizeBlankToNull(request.name()),
            normalizeBlankToNull(request.specUrl()),
            request.active()
        );
        return ApiSpecSourceResponse.from(source);
    }

    @Transactional
    public void deleteSource(Long sourceId) {
        ApiSpecSource source = getSourceWithWriteCheck(sourceId);
        source.softDelete();
    }

    @Transactional
    public ApiSpecSourceResponse toggleSource(Long sourceId) {
        ApiSpecSource source = getSourceWithWriteCheck(sourceId);
        source.toggleActive();
        return ApiSpecSourceResponse.from(source);
    }

    @Transactional
    public ApiSpecDiffDetailResponse checkSource(Long sourceId) {
        ApiSpecSource source = getSourceWithWriteCheck(sourceId);
        return checkLoadedSource(source);
    }

    @Transactional
    public ApiSpecDiffDetailResponse checkActiveSource(Long sourceId) {
        ApiSpecSource source = specSourceRepository.findByIdAndDeletedFalse(sourceId)
            .orElseThrow(() -> new ApiSpecNotFoundException("API 스펙 소스를 찾을 수 없습니다."));
        return checkLoadedSource(source);
    }

    private ApiSpecDiffDetailResponse checkLoadedSource(ApiSpecSource source) {
        if (!source.isActive()) {
            throw new IllegalArgumentException("비활성화된 OpenAPI 소스는 검사할 수 없습니다.");
        }
        ApiSpecSnapshot baseSnapshot = specSnapshotRepository
            .findFirstBySpecSourceIdOrderByCapturedAtDesc(source.getId())
            .orElse(null);

        JsonNode headSpec = fetchSpec(source.getSpecUrl());
        String normalizedSpec = normalize(headSpec);
        String contentHash = sha256(normalizedSpec);

        ApiSpecSnapshot headSnapshot = baseSnapshot;
        if (baseSnapshot == null || !baseSnapshot.getContentHash().equals(contentHash)) {
            headSnapshot = specSnapshotRepository.save(ApiSpecSnapshot.builder()
                .specSource(source)
                .contentHash(contentHash)
                .rawSpec(normalizedSpec)
                .capturedAt(LocalDateTime.now())
                .build());
        }

        List<BreakingChange> changes = new ArrayList<>();
        if (baseSnapshot != null && !baseSnapshot.getContentHash().equals(contentHash)) {
            JsonNode baseSpec = parse(baseSnapshot.getRawSpec());
            changes = detectBreakingChanges(baseSpec, headSpec);
        }

        source.markChecked();

        ApiSpecDiff diff = specDiffRepository.save(ApiSpecDiff.builder()
            .specSource(source)
            .baseSnapshot(baseSnapshot)
            .headSnapshot(headSnapshot)
            .breaking(!changes.isEmpty())
            .breakingChangeCount(changes.size())
            .summary(buildSummary(baseSnapshot, changes, baseSnapshot != null && baseSnapshot.getContentHash().equals(contentHash)))
            .checkedAt(LocalDateTime.now())
            .build());

        List<BreakingChange> savedChanges = changes.stream()
            .map(change -> BreakingChange.builder()
                .diff(diff)
                .rule(change.getRule())
                .location(change.getLocation())
                .description(change.getDescription())
                .build())
            .toList();
        breakingChangeRepository.saveAll(savedChanges);
        incidentService.recordContractChange(source.getProject(), source.getName(), savedChanges);

        return ApiSpecDiffDetailResponse.from(
            diff,
            savedChanges.stream().map(BreakingChangeResponse::from).toList()
        );
    }

    public List<ApiSpecDiffResponse> getDiffs(Long sourceId) {
        getSourceWithAccessCheck(sourceId);
        return specDiffRepository.findBySpecSourceIdOrderByCheckedAtDesc(sourceId).stream()
            .map(ApiSpecDiffResponse::from)
            .toList();
    }

    public ApiSpecDiffDetailResponse getDiff(Long diffId) {
        ApiSpecDiff diff = specDiffRepository.findById(diffId)
            .orElseThrow(() -> new ApiSpecNotFoundException("API 스펙 변경 이력을 찾을 수 없습니다."));
        projectService.getProjectWithAccessCheck(diff.getSpecSource().getProject().getId());

        List<BreakingChangeResponse> changes = breakingChangeRepository.findByDiffIdOrderByIdAsc(diffId)
            .stream()
            .map(BreakingChangeResponse::from)
            .toList();

        return ApiSpecDiffDetailResponse.from(diff, changes);
    }

    private ApiSpecSource getSourceWithAccessCheck(Long sourceId) {
        ApiSpecSource source = specSourceRepository.findByIdAndDeletedFalse(sourceId)
            .orElseThrow(() -> new ApiSpecNotFoundException("API 스펙 소스를 찾을 수 없습니다."));
        projectService.getProjectWithAccessCheck(source.getProject().getId());
        return source;
    }

    private ApiSpecSource getSourceWithWriteCheck(Long sourceId) {
        ApiSpecSource source = specSourceRepository.findByIdAndDeletedFalse(sourceId)
            .orElseThrow(() -> new ApiSpecNotFoundException("API 스펙 소스를 찾을 수 없습니다."));
        projectService.getProjectWithMemberCheck(source.getProject().getId());
        return source;
    }

    private JsonNode fetchSpec(String specUrl) {
        try {
            URI uri = outboundUrlGuard.validateHttpUrl(specUrl, "OpenAPI 스펙 URL");
            String raw = restTemplate.getForObject(uri, String.class);
            if (raw == null || raw.isBlank()) {
                throw new IllegalArgumentException("OpenAPI 스펙 응답이 비어 있습니다.");
            }
            return parse(raw);
        } catch (RestClientException e) {
            throw new IllegalArgumentException("OpenAPI 스펙을 가져올 수 없습니다: " + e.getMessage());
        }
    }

    private JsonNode parse(String rawSpec) {
        try {
            JsonNode root = objectMapper.readTree(rawSpec);
            if (!root.has("openapi") || !root.has("paths")) {
                throw new IllegalArgumentException("OpenAPI 3 JSON 문서만 지원합니다.");
            }
            return root;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("OpenAPI 스펙 JSON을 파싱할 수 없습니다.");
        }
    }

    private String normalize(JsonNode spec) {
        try {
            return objectMapper.writeValueAsString(spec);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("OpenAPI 스펙을 정규화할 수 없습니다.");
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(encodedHash.length * 2);
            for (byte b : encodedHash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private List<BreakingChange> detectBreakingChanges(JsonNode baseSpec, JsonNode headSpec) {
        List<BreakingChange> changes = new ArrayList<>();
        JsonNode basePaths = baseSpec.path("paths");
        JsonNode headPaths = headSpec.path("paths");

        detectRemovedPaths(basePaths, headPaths, changes);
        detectRemovedMethods(basePaths, headPaths, changes);
        detectAddedRequiredParameters(baseSpec, headSpec, basePaths, headPaths, changes);
        detectRequestBodyChanges(baseSpec, headSpec, basePaths, headPaths, changes);
        detectResponseSchemaChanges(baseSpec, headSpec, basePaths, headPaths, changes);

        return changes;
    }

    private void detectRemovedPaths(JsonNode basePaths, JsonNode headPaths, List<BreakingChange> changes) {
        Iterator<String> basePathNames = basePaths.fieldNames();
        while (basePathNames.hasNext()) {
            String path = basePathNames.next();
            if (!headPaths.has(path)) {
                changes.add(change(
                    BreakingChangeRule.PATH_REMOVED,
                    path,
                    "기존 path가 삭제되었습니다."
                ));
            }
        }
    }

    private void detectRemovedMethods(JsonNode basePaths, JsonNode headPaths, List<BreakingChange> changes) {
        Iterator<String> basePathNames = basePaths.fieldNames();
        while (basePathNames.hasNext()) {
            String path = basePathNames.next();
            if (!headPaths.has(path)) {
                continue;
            }

            JsonNode basePath = basePaths.path(path);
            JsonNode headPath = headPaths.path(path);
            Iterator<String> fields = basePath.fieldNames();
            while (fields.hasNext()) {
                String method = fields.next();
                if (isHttpMethod(method) && !headPath.has(method)) {
                    changes.add(change(
                        BreakingChangeRule.METHOD_REMOVED,
                        path + " " + method.toUpperCase(),
                        "기존 method가 삭제되었습니다."
                    ));
                }
            }
        }
    }

    private void detectAddedRequiredParameters(
        JsonNode baseSpec,
        JsonNode headSpec,
        JsonNode basePaths,
        JsonNode headPaths,
        List<BreakingChange> changes
    ) {
        forEachCommonOperation(basePaths, headPaths, (path, method, baseOperation, headOperation) -> {
            Map<String, JsonNode> baseParameters = collectParameters(baseSpec, basePaths.path(path), baseOperation);
            Map<String, JsonNode> headParameters = collectParameters(headSpec, headPaths.path(path), headOperation);

            for (Map.Entry<String, JsonNode> entry : headParameters.entrySet()) {
                if (baseParameters.containsKey(entry.getKey())) {
                    continue;
                }

                JsonNode parameter = entry.getValue();
                if (parameter.path("required").asBoolean(false)) {
                    changes.add(change(
                        BreakingChangeRule.REQUIRED_PARAMETER_ADDED,
                        path + " " + method.toUpperCase() + " parameter " + entry.getKey(),
                        "기존 요청에 없던 필수 request parameter가 추가되었습니다."
                    ));
                }
            }
        });
    }

    private void detectRequestBodyChanges(
        JsonNode baseSpec,
        JsonNode headSpec,
        JsonNode basePaths,
        JsonNode headPaths,
        List<BreakingChange> changes
    ) {
        forEachCommonOperation(basePaths, headPaths, (path, method, baseOperation, headOperation) -> {
            JsonNode headRequestBody = resolveNode(headSpec, headOperation.path("requestBody"));
            if (headRequestBody.isMissingNode()) {
                return;
            }

            JsonNode baseRequestBody = resolveNode(baseSpec, baseOperation.path("requestBody"));
            boolean baseRequired = !baseRequestBody.isMissingNode()
                && baseRequestBody.path("required").asBoolean(false);
            boolean headRequired = headRequestBody.path("required").asBoolean(false);

            if (!baseRequired && headRequired) {
                changes.add(change(
                    BreakingChangeRule.REQUIRED_REQUEST_BODY_ADDED,
                    path + " " + method.toUpperCase() + " requestBody",
                    "기존 요청에 없던 필수 request body가 추가되었습니다."
                ));
            }

            JsonNode headContent = headRequestBody.path("content");
            if (!headContent.isObject()) {
                return;
            }

            JsonNode baseContent = baseRequestBody.path("content");
            Iterator<String> mediaTypes = headContent.fieldNames();
            while (mediaTypes.hasNext()) {
                String mediaType = mediaTypes.next();
                if (!baseContent.has(mediaType) && !headRequired) {
                    continue;
                }

                compareRequestRequiredFields(
                    baseSpec,
                    headSpec,
                    baseContent.path(mediaType).path("schema"),
                    headContent.path(mediaType).path("schema"),
                    path + " " + method.toUpperCase() + " requestBody " + mediaType,
                    changes,
                    0
                );
            }
        });
    }

    private void detectResponseSchemaChanges(
        JsonNode baseSpec,
        JsonNode headSpec,
        JsonNode basePaths,
        JsonNode headPaths,
        List<BreakingChange> changes
    ) {
        forEachCommonOperation(basePaths, headPaths, (path, method, baseOperation, headOperation) -> {
            JsonNode baseResponses = baseOperation.path("responses");
            JsonNode headResponses = headOperation.path("responses");
            Iterator<String> statusCodes = baseResponses.fieldNames();

            while (statusCodes.hasNext()) {
                String statusCode = statusCodes.next();
                if (!headResponses.has(statusCode)) {
                    continue;
                }

                JsonNode baseContent = resolveNode(baseSpec, baseResponses.path(statusCode)).path("content");
                JsonNode headContent = resolveNode(headSpec, headResponses.path(statusCode)).path("content");
                Iterator<String> mediaTypes = baseContent.fieldNames();

                while (mediaTypes.hasNext()) {
                    String mediaType = mediaTypes.next();
                    if (!headContent.has(mediaType)) {
                        continue;
                    }

                    String location = path + " " + method.toUpperCase()
                        + " responses." + statusCode + " " + mediaType;
                    compareSchemas(
                        baseSpec,
                        headSpec,
                        baseContent.path(mediaType).path("schema"),
                        headContent.path(mediaType).path("schema"),
                        location,
                        changes,
                        0
                    );
                }
            }
        });
    }

    private void compareRequestRequiredFields(
        JsonNode baseRoot,
        JsonNode headRoot,
        JsonNode baseSchemaNode,
        JsonNode headSchemaNode,
        String location,
        List<BreakingChange> changes,
        int depth
    ) {
        if (depth > 12 || headSchemaNode.isMissingNode()) {
            return;
        }

        JsonNode baseSchema = resolveNode(baseRoot, baseSchemaNode);
        JsonNode headSchema = resolveNode(headRoot, headSchemaNode);

        String baseType = schemaType(baseRoot, baseSchema);
        String headType = schemaType(headRoot, headSchema);
        if ("array".equals(baseType) || "array".equals(headType)) {
            compareRequestRequiredFields(
                baseRoot,
                headRoot,
                baseSchema.path("items"),
                headSchema.path("items"),
                location + "[]",
                changes,
                depth + 1
            );
            return;
        }

        JsonNode headProperties = headSchema.path("properties");
        if (!headProperties.isObject()) {
            return;
        }

        Set<String> baseRequiredFields = requiredFields(baseSchema);
        Set<String> headRequiredFields = requiredFields(headSchema);
        for (String fieldName : headRequiredFields) {
            if (!baseRequiredFields.contains(fieldName)) {
                changes.add(change(
                    BreakingChangeRule.REQUEST_BODY_REQUIRED_FIELD_ADDED,
                    location + "." + fieldName,
                    "기존 요청에 없던 필수 request body field가 추가되었습니다."
                ));
            }
        }

        JsonNode baseProperties = baseSchema.path("properties");
        Iterator<String> propertyNames = headProperties.fieldNames();
        while (propertyNames.hasNext()) {
            String propertyName = propertyNames.next();
            if (!baseProperties.has(propertyName)) {
                continue;
            }

            compareRequestRequiredFields(
                baseRoot,
                headRoot,
                baseProperties.path(propertyName),
                headProperties.path(propertyName),
                location + "." + propertyName,
                changes,
                depth + 1
            );
        }
    }

    private Map<String, JsonNode> collectParameters(JsonNode root, JsonNode pathItem, JsonNode operation) {
        Map<String, JsonNode> parameters = new HashMap<>();
        addParameters(root, parameters, pathItem.path("parameters"));
        addParameters(root, parameters, operation.path("parameters"));
        return parameters;
    }

    private void addParameters(JsonNode root, Map<String, JsonNode> parameters, JsonNode parameterArray) {
        if (!parameterArray.isArray()) {
            return;
        }
        for (JsonNode parameterNode : parameterArray) {
            JsonNode parameter = resolveNode(root, parameterNode);
            String in = parameter.path("in").asText("");
            String name = parameter.path("name").asText("");
            if (!in.isBlank() && !name.isBlank()) {
                parameters.put(in + ":" + name, parameter);
            }
        }
    }

    private void compareSchemas(
        JsonNode baseRoot,
        JsonNode headRoot,
        JsonNode baseSchemaNode,
        JsonNode headSchemaNode,
        String location,
        List<BreakingChange> changes,
        int depth
    ) {
        if (depth > 12 || baseSchemaNode.isMissingNode() || headSchemaNode.isMissingNode()) {
            return;
        }

        JsonNode baseSchema = resolveNode(baseRoot, baseSchemaNode);
        JsonNode headSchema = resolveNode(headRoot, headSchemaNode);
        String baseType = schemaType(baseRoot, baseSchema);
        String headType = schemaType(headRoot, headSchema);

        if (!baseType.isBlank() && !headType.isBlank() && !baseType.equals(headType)) {
            changes.add(change(
                BreakingChangeRule.RESPONSE_FIELD_TYPE_CHANGED,
                location,
                "응답 schema field type이 " + baseType + "에서 " + headType + "(으)로 변경되었습니다."
            ));
            return;
        }

        if ("array".equals(baseType) || "array".equals(headType)) {
            compareSchemas(
                baseRoot,
                headRoot,
                baseSchema.path("items"),
                headSchema.path("items"),
                location + "[]",
                changes,
                depth + 1
            );
            return;
        }

        JsonNode baseProperties = baseSchema.path("properties");
        JsonNode headProperties = headSchema.path("properties");
        if (!baseProperties.isObject() || !headProperties.isObject()) {
            return;
        }

        Iterator<String> propertyNames = baseProperties.fieldNames();
        while (propertyNames.hasNext()) {
            String propertyName = propertyNames.next();
            String propertyLocation = location + "." + propertyName;
            if (!headProperties.has(propertyName)) {
                changes.add(change(
                    BreakingChangeRule.RESPONSE_FIELD_REMOVED,
                    propertyLocation,
                    "응답 schema field가 삭제되었습니다."
                ));
                continue;
            }

            compareSchemas(
                baseRoot,
                headRoot,
                baseProperties.path(propertyName),
                headProperties.path(propertyName),
                propertyLocation,
                changes,
                depth + 1
            );
        }
    }

    private String schemaType(JsonNode root, JsonNode schemaNode) {
        JsonNode schema = resolveNode(root, schemaNode);
        if (schema.path("type").isTextual()) {
            return schema.path("type").asText();
        }
        if (schema.has("properties")) {
            return "object";
        }
        if (schema.has("items")) {
            return "array";
        }
        return "";
    }

    private Set<String> requiredFields(JsonNode schema) {
        Set<String> fields = new HashSet<>();
        JsonNode required = schema.path("required");
        if (!required.isArray()) {
            return fields;
        }

        for (JsonNode field : required) {
            if (field.isTextual()) {
                fields.add(field.asText());
            }
        }
        return fields;
    }

    private JsonNode resolveNode(JsonNode root, JsonNode node) {
        if (node.has("$ref")) {
            String ref = node.path("$ref").asText();
            if (ref.startsWith("#")) {
                JsonNode resolved = root.at(ref.substring(1));
                if (!resolved.isMissingNode()) {
                    return resolved;
                }
            }
        }
        return node;
    }

    private boolean isHttpMethod(String fieldName) {
        return HTTP_METHODS.contains(fieldName.toLowerCase());
    }

    private void forEachCommonOperation(
        JsonNode basePaths,
        JsonNode headPaths,
        OperationConsumer consumer
    ) {
        Iterator<String> pathNames = basePaths.fieldNames();
        while (pathNames.hasNext()) {
            String path = pathNames.next();
            if (!headPaths.has(path)) {
                continue;
            }

            JsonNode basePath = basePaths.path(path);
            JsonNode headPath = headPaths.path(path);
            Iterator<String> fields = basePath.fieldNames();
            while (fields.hasNext()) {
                String method = fields.next();
                if (isHttpMethod(method) && headPath.has(method)) {
                    consumer.accept(path, method, basePath.path(method), headPath.path(method));
                }
            }
        }
    }

    private BreakingChange change(BreakingChangeRule rule, String location, String description) {
        return BreakingChange.builder()
            .rule(rule)
            .location(location)
            .description(description)
            .build();
    }

    private String buildSummary(
        ApiSpecSnapshot baseSnapshot,
        List<BreakingChange> changes,
        boolean unchanged
    ) {
        if (baseSnapshot == null) {
            return "Initial OpenAPI snapshot captured.";
        }
        if (unchanged) {
            return "No spec changes detected.";
        }
        if (changes.isEmpty()) {
            return "Spec changed, but no configured breaking changes were detected.";
        }
        return "Detected " + changes.size() + " breaking change(s).";
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @FunctionalInterface
    private interface OperationConsumer {
        void accept(String path, String method, JsonNode baseOperation, JsonNode headOperation);
    }
}
