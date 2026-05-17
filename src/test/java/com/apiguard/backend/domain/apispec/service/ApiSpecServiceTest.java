package com.apiguard.backend.domain.apispec.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.apiguard.backend.domain.apispec.dto.ApiSpecDiffDetailResponse;
import com.apiguard.backend.domain.apispec.entity.ApiSpecDiff;
import com.apiguard.backend.domain.apispec.entity.ApiSpecSnapshot;
import com.apiguard.backend.domain.apispec.entity.ApiSpecSource;
import com.apiguard.backend.domain.apispec.entity.BreakingChangeRule;
import com.apiguard.backend.domain.apispec.repository.ApiSpecDiffRepository;
import com.apiguard.backend.domain.apispec.repository.ApiSpecSnapshotRepository;
import com.apiguard.backend.domain.apispec.repository.ApiSpecSourceRepository;
import com.apiguard.backend.domain.apispec.repository.BreakingChangeRepository;
import com.apiguard.backend.domain.incident.service.IncidentService;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.service.ProjectService;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ApiSpecServiceTest {

    @Mock
    private ApiSpecSourceRepository specSourceRepository;

    @Mock
    private ApiSpecSnapshotRepository specSnapshotRepository;

    @Mock
    private ApiSpecDiffRepository specDiffRepository;

    @Mock
    private BreakingChangeRepository breakingChangeRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private IncidentService incidentService;

    private ApiSpecService apiSpecService;

    @BeforeEach
    void setUp() {
        apiSpecService = new ApiSpecService(
            specSourceRepository,
            specSnapshotRepository,
            specDiffRepository,
            breakingChangeRepository,
            projectService,
            restTemplate,
            new ObjectMapper(),
            incidentService
        );
    }

    @Test
    @DisplayName("OpenAPI 변경 검사 - path/method/request/response schema breaking change 감지")
    void checkSource_detectsBreakingChanges() {
        User user = User.builder()
            .id(1L)
            .email("test@example.com")
            .nickname("tester")
            .password("encoded")
            .role(Role.USER)
            .build();
        Project project = Project.builder()
            .id(1L)
            .user(user)
            .name("Payments")
            .build();
        ApiSpecSource source = ApiSpecSource.builder()
            .id(1L)
            .project(project)
            .name("Payments API")
            .specUrl("https://example.com/openapi.json")
            .build();
        ApiSpecSnapshot baseSnapshot = ApiSpecSnapshot.builder()
            .id(1L)
            .specSource(source)
            .contentHash("old")
            .rawSpec(baseSpec())
            .capturedAt(LocalDateTime.now().minusDays(1))
            .build();

        given(specSourceRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(source));
        given(projectService.getProjectWithMemberCheck(1L)).willReturn(project);
        given(specSnapshotRepository.findFirstBySpecSourceIdOrderByCapturedAtDesc(1L))
            .willReturn(Optional.of(baseSnapshot));
        given(restTemplate.getForObject("https://example.com/openapi.json", String.class))
            .willReturn(headSpec());
        given(specSnapshotRepository.save(any(ApiSpecSnapshot.class))).willAnswer(invocation -> {
            ApiSpecSnapshot snapshot = invocation.getArgument(0);
            return ApiSpecSnapshot.builder()
                .id(2L)
                .specSource(snapshot.getSpecSource())
                .contentHash(snapshot.getContentHash())
                .rawSpec(snapshot.getRawSpec())
                .capturedAt(snapshot.getCapturedAt())
                .build();
        });
        given(specDiffRepository.save(any(ApiSpecDiff.class))).willAnswer(invocation -> {
            ApiSpecDiff diff = invocation.getArgument(0);
            return ApiSpecDiff.builder()
                .id(1L)
                .specSource(diff.getSpecSource())
                .baseSnapshot(diff.getBaseSnapshot())
                .headSnapshot(diff.getHeadSnapshot())
                .breaking(diff.isBreaking())
                .breakingChangeCount(diff.getBreakingChangeCount())
                .summary(diff.getSummary())
                .checkedAt(diff.getCheckedAt())
                .build();
        });

        ApiSpecDiffDetailResponse response = apiSpecService.checkSource(1L);

        assertThat(response.breaking()).isTrue();
        assertThat(response.breakingChangeCount()).isEqualTo(7);
        assertThat(response.changes())
            .extracting("rule")
            .containsExactlyInAnyOrder(
                BreakingChangeRule.PATH_REMOVED,
                BreakingChangeRule.METHOD_REMOVED,
                BreakingChangeRule.REQUIRED_PARAMETER_ADDED,
                BreakingChangeRule.REQUIRED_REQUEST_BODY_ADDED,
                BreakingChangeRule.REQUEST_BODY_REQUIRED_FIELD_ADDED,
                BreakingChangeRule.RESPONSE_FIELD_REMOVED,
                BreakingChangeRule.RESPONSE_FIELD_TYPE_CHANGED
            );
        verify(incidentService).recordContractChange(eq(project), eq("Payments API"), any());
    }

    private String baseSpec() {
        return """
            {
              "openapi": "3.0.3",
              "info": {"title": "Test API", "version": "1.0.0"},
              "paths": {
                "/legacy": {
                  "get": {
                    "responses": {"200": {"description": "ok"}}
                  }
                },
                "/users": {
                  "get": {
                    "parameters": [
                      {"name": "page", "in": "query", "required": false, "schema": {"type": "integer"}}
                    ],
                    "responses": {
                      "200": {
                        "description": "ok",
                        "content": {
                          "application/json": {
                            "schema": {
                              "type": "object",
                              "properties": {
                                "id": {"type": "string"},
                                "name": {"type": "string"}
                              }
                            }
                          }
                        }
                      }
                    }
                  },
                  "post": {
                    "requestBody": {
                      "required": false,
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "properties": {
                              "name": {"type": "string"}
                            }
                          }
                        }
                      }
                    },
                    "responses": {"201": {"description": "created"}}
                  },
                  "delete": {
                    "responses": {"204": {"description": "deleted"}}
                  }
                }
              }
            }
            """;
    }

    private String headSpec() {
        return """
            {
              "openapi": "3.0.3",
              "info": {"title": "Test API", "version": "2.0.0"},
              "paths": {
                "/users": {
                  "get": {
                    "parameters": [
                      {"name": "page", "in": "query", "required": false, "schema": {"type": "integer"}},
                      {"name": "limit", "in": "query", "required": true, "schema": {"type": "integer"}}
                    ],
                    "responses": {
                      "200": {
                        "description": "ok",
                        "content": {
                          "application/json": {
                            "schema": {
                              "type": "object",
                              "properties": {
                                "id": {"type": "integer"}
                              }
                            }
                          }
                        }
                      }
                    }
                  },
                  "post": {
                    "requestBody": {
                      "required": true,
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "required": ["email"],
                            "properties": {
                              "name": {"type": "string"},
                              "email": {"type": "string"}
                            }
                          }
                        }
                      }
                    },
                    "responses": {"201": {"description": "created"}}
                  }
                }
              }
            }
            """;
    }
}
