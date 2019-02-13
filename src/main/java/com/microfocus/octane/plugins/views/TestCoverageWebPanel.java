/*
 *     Copyright 2018 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.microfocus.octane.plugins.views;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.google.gson.Gson;
import com.microfocus.octane.plugins.components.api.OctaneRestService;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;
import com.microfocus.octane.plugins.configuration.WorkspaceConfiguration;
import com.microfocus.octane.plugins.rest.RestStatusException;
import com.microfocus.octane.plugins.rest.entities.MapBasedObject;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntity;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;
import com.microfocus.octane.plugins.rest.query.InQueryPhrase;
import com.microfocus.octane.plugins.rest.query.QueryPhrase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Scanned
public class TestCoverageWebPanel extends AbstractJiraContextProvider {

    private static final Logger log = LoggerFactory.getLogger(TestCoverageWebPanel.class);
    private OctaneConfigurationManager configurationManager = OctaneConfigurationManager.getInstance();
    private OctaneRestService octaneRestService;
    private NumberFormat countFormat = NumberFormat.getInstance();
    private NumberFormat percentFormatter = NumberFormat.getPercentInstance();
    private Map<String, TypeDescriptor> typeDescriptors = new HashMap<>();
    private Map<String, TestStatusDescriptor> testStatusByNameDescriptors = new HashMap<>();
    private Map<String, TestStatusDescriptor> testStatusByLogicalNameDescriptors = new HashMap<>();
    private final static String UDF_NOT_DEFINED_IN_OCTANE = "platform.unknown_field";

    public TestCoverageWebPanel(OctaneRestService octaneRestService) {
        this.octaneRestService = octaneRestService;
        percentFormatter.setMinimumFractionDigits(1);
        percentFormatter.setMinimumFractionDigits(1);

        //TYPE
        TypeDescriptor applicationModuleType = new TypeDescriptor("application_module", "AM", "#43e4ff", "product_area");
        TypeDescriptor featureType = new TypeDescriptor("feature", "F", "#e57828", "work_item");
        TypeDescriptor storyType = new TypeDescriptor("story", "US", "#ffb000", "work_item");
        typeDescriptors.put(applicationModuleType.getTypeName(), applicationModuleType);
        typeDescriptors.put(featureType.getTypeName(), featureType);
        typeDescriptors.put(storyType.getTypeName(), storyType);


        //TEST TYPES
        TestStatusDescriptor passedStatus = new TestStatusDescriptor("passed", "list_node.run_status.passed", "Passed", "rgb(26, 172, 96)", 1);
        TestStatusDescriptor failedStatus = new TestStatusDescriptor("failed", "list_node.run_status.failed", "Failed", "rgb(229, 0, 76)", 2);
        TestStatusDescriptor plannedStatus = new TestStatusDescriptor("planned", "list_node.run_status.planned", "Planned", "#dddddd" /*"rgb(47, 214, 195)"*/, 3);
        TestStatusDescriptor skippedStatus = new TestStatusDescriptor("skipped", "list_node.run_status.skipped", "Skipped", "rgb(82, 22, 172)", 4);
        TestStatusDescriptor needAttentionStatus = new TestStatusDescriptor("needsAttention", "list_node.run_status.requires_attention", "Requires Attention", "rgb(252, 219, 31)", 5);
        TestStatusDescriptor testNoRunStatus = new TestStatusDescriptor("testNoRun", null, "Tests did not run", "#cccccc", 6);

        //BY NAME
        testStatusByNameDescriptors.put(passedStatus.getName(), passedStatus);
        testStatusByNameDescriptors.put(failedStatus.getName(), failedStatus);
        testStatusByNameDescriptors.put(plannedStatus.getName(), plannedStatus);
        testStatusByNameDescriptors.put(skippedStatus.getName(), skippedStatus);
        testStatusByNameDescriptors.put(needAttentionStatus.getName(), needAttentionStatus);
        testStatusByNameDescriptors.put(testNoRunStatus.getName(), testNoRunStatus);

        //BY LOGICAL NAME
        testStatusByLogicalNameDescriptors.put(passedStatus.getLogicalName(), passedStatus);
        testStatusByLogicalNameDescriptors.put(failedStatus.getLogicalName(), failedStatus);
        testStatusByLogicalNameDescriptors.put(plannedStatus.getLogicalName(), plannedStatus);
        testStatusByLogicalNameDescriptors.put(skippedStatus.getLogicalName(), skippedStatus);
        testStatusByLogicalNameDescriptors.put(needAttentionStatus.getLogicalName(), needAttentionStatus);

    }

    @Override
    public Map<String, Object> getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        Map<String, Object> contextMap = new HashMap<>();
        log.trace("configurationManager.isValidConfiguration() = " + configurationManager.isValidConfiguration());
        if (configurationManager.isValidConfiguration()) {
            try {
                WorkspaceConfiguration workspaceConfig = configurationManager.getConfiguration().getWorkspaces().stream()
                        .filter(w -> w.getJiraProjects().contains(jiraHelper.getProject().getKey())).findFirst().get();//we use get without validation because validation is done in condition
                Issue currentIssue = (Issue) jiraHelper.getContextParams().get("issue");

                QueryPhrase jiraKeyCondition = new InQueryPhrase(workspaceConfig.getOctaneUdf(), Arrays.asList(currentIssue.getKey(), currentIssue.getId().toString()));
                boolean found = false;
                if (workspaceConfig.getOctaneEntityTypes().contains(OctaneRestService.OCTANE_ENTITY_FEATURE) || workspaceConfig.getOctaneEntityTypes().contains(OctaneRestService.OCTANE_ENTITY_USER_STORY)) {
                    found = tryGetWorkItemEntity(workspaceConfig, contextMap, jiraKeyCondition);
                }
                if (!found && workspaceConfig.getOctaneEntityTypes().contains(OctaneRestService.OCTANE_ENTITY_APPLICATION_MODULE)) {
                    found = tryGetApplicationModuleEntity(workspaceConfig, contextMap, jiraKeyCondition);
                }
                if (!found) {
                    contextMap.put("status", "noData");
                } else {
                    //context map is filled
                }
            } catch (RestStatusException e) {
                if (e.getResponse().getStatusCode() == 401) {
                    //credentials issue
                } else {
                    log.error("Failed to fill ContextMap (1) : " + e.getMessage());
                }
            } catch (Exception e) {
                log.error("Failed to fill ContextMap (2) : " + e.getMessage());
            }
        }

        if (!contextMap.containsKey("status")) {
            contextMap.put("status", "noValidConfiguration");
            String configUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + "/plugins/servlet/admin/octane";
            contextMap.put("configUrl", configUrl);
        }

        return contextMap;
    }

    private boolean tryGetApplicationModuleEntity(WorkspaceConfiguration workspaceConfiguration, Map<String, Object> contextMap, QueryPhrase jiraKeyCondition) {
        try {
            //CHECK Application modules
            OctaneEntityCollection applicationModules = octaneRestService.getEntitiesByCondition(workspaceConfiguration.getWorkspaceId(), "application_modules", Arrays.asList(jiraKeyCondition), Arrays.asList("path", "name"));
            if (!applicationModules.getData().isEmpty()) {

                OctaneEntity octaneEntity = applicationModules.getData().get(0);
                TypeDescriptor typeDescriptor = typeDescriptors.get(octaneEntity.getType());
                String path = octaneEntity.getString("path");

                GroupEntityCollection coverage = octaneRestService.getCoverageForApplicationModule(path, workspaceConfiguration.getWorkspaceId());
                int total = coverage.getGroups().stream().filter(gr -> gr.getValue() != null).mapToInt(o -> o.getCount()).sum();
                List<MapBasedObject> groups = coverage.getGroups().stream().filter(gr -> gr.getValue() != null).map(gr -> convertGroupEntityToUiEntity(gr, total))
                        .sorted(Comparator.comparing(a -> (Integer) a.get("order"))).collect(Collectors.toList());
                fillContextMapWithEntity(contextMap, workspaceConfiguration, typeDescriptor, octaneEntity, groups, total);
                return true;
            }
        } catch (RestStatusException e) {
            if (UDF_NOT_DEFINED_IN_OCTANE.equals(e.getErrorCode())) {
                //field is not defined - skip
            } else {
                throw e;
            }
        }
        return false;
    }

    private boolean tryGetWorkItemEntity(WorkspaceConfiguration workspaceConfiguration, Map<String, Object> contextMap, QueryPhrase jiraKeyCondition) {
        try {
            //CHECK WORKING ITEM
            QueryPhrase subTypeCondition = new InQueryPhrase("subtype", Arrays.asList("story", "feature"));
            OctaneEntityCollection workItems = octaneRestService.getEntitiesByCondition(workspaceConfiguration.getWorkspaceId(), "work_items",
                    Arrays.asList(jiraKeyCondition, subTypeCondition), Arrays.asList("subtype", "name", "last_runs"));
            if (!workItems.getData().isEmpty()) {
                OctaneEntity octaneEntity = workItems.getData().get(0);

                TypeDescriptor typeDescriptor = typeDescriptors.get(octaneEntity.getString("subtype"));
                String lastRuns = octaneEntity.getString("last_runs");
                Map<String, Double> name2countStatuses = new Gson().fromJson(lastRuns, Map.class);
                int total = name2countStatuses.values().stream().mapToInt(a -> a.intValue()).sum();
                List<MapBasedObject> groups = name2countStatuses.entrySet().stream().filter(entry -> entry.getValue() > 0)
                        .map(entry -> convertGroupEntityToUiEntity(entry.getKey(), entry.getValue().intValue(), total))
                        .sorted(Comparator.comparing(a -> (Integer) a.get("order"))).collect(Collectors.toList());
                fillContextMapWithEntity(contextMap, workspaceConfiguration, typeDescriptor, octaneEntity, groups, total);
                return true;
            }
        } catch (RestStatusException e) {
            if (UDF_NOT_DEFINED_IN_OCTANE.equals(e.getErrorCode())) {
                //field is not defined - skip
            } else {
                throw e;
            }
        }
        return false;
    }

    private void fillContextMapWithEntity(Map<String, Object> contextMap, WorkspaceConfiguration workspaceConfiguration, TypeDescriptor typeDescriptor, OctaneEntity octaneEntity, List<MapBasedObject> groups, int total) {
        //TODO revert comment
        octaneEntity.put("url", typeDescriptor.buildEntityUrl(workspaceConfiguration.getSpaceConfiguration().getLocationParts().getBaseUrl(),
                workspaceConfiguration.getSpaceConfiguration().getLocationParts().getSpaceId(), workspaceConfiguration.getWorkspaceId(), octaneEntity.getId()));
        octaneEntity.put("typeKey", typeDescriptor.getTypeKey());
        octaneEntity.put("typeColor", typeDescriptor.getTypeColor());
        contextMap.put("total", total);
        contextMap.put("groups", groups);
        contextMap.put("status", "hasData");
        contextMap.put("octaneEntity", octaneEntity);
        contextMap.put("hasData", true);

    }

    private MapBasedObject convertGroupEntityToUiEntity(String statusName, int count, int totalCount) {
        TestStatusDescriptor testStatusDescriptor = testStatusByNameDescriptors.get(statusName);
        return convertGroupEntityToUiEntity(testStatusDescriptor.getTitle(), testStatusDescriptor.getOrder(), testStatusDescriptor.getColor(), count, totalCount);
    }

    private MapBasedObject convertGroupEntityToUiEntity(GroupEntity groupEntity, int totalCount) {
        TestStatusDescriptor testStatusDescriptor = testStatusByLogicalNameDescriptors.get(groupEntity.getValue().getId());
        return convertGroupEntityToUiEntity(testStatusDescriptor.getTitle(), testStatusDescriptor.getOrder(), testStatusDescriptor.getColor(), groupEntity.getCount(), totalCount);
    }

    private MapBasedObject convertGroupEntityToUiEntity(String groupName, int order, String color, int count, int totalCount) {
        MapBasedObject outputEntity = new MapBasedObject();
        outputEntity.put("color", color);
        outputEntity.put("order", order);
        outputEntity.put("count", countFormat.format(count));
        outputEntity.put("name", groupName);

        float percentage = 1f * count / totalCount;
        String percentageAsString = percentFormatter.format(percentage);//String.format("%.1f", percentage);
        outputEntity.put("percentage", percentageAsString);
        return outputEntity;
    }

    public class TestStatusDescriptor {
        private String name;
        private String logicalName;
        private String title;
        private String color;
        private int order;

        public TestStatusDescriptor(String name, String logicalName, String title, String color, int order) {
            this.name = name;
            this.logicalName = logicalName;
            this.title = title;
            this.color = color;
            this.order = order;
        }

        public String getTitle() {
            return title;
        }


        public String getName() {
            return name;
        }

        public String getLogicalName() {
            return logicalName;
        }

        public String getColor() {
            return color;
        }

        public int getOrder() {
            return order;
        }
    }

    public class TypeDescriptor {
        private String typeName;
        private String typeKey;
        private String typeColor;
        private String nameForNavigation;

        public TypeDescriptor(String typeName, String typeKey, String typeColor, String nameForNavigation) {
            this.typeName = typeName;
            this.typeKey = typeKey;
            this.typeColor = typeColor;
            this.nameForNavigation = nameForNavigation;
        }

        public String getTypeKey() {
            return typeKey;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getTypeColor() {
            return typeColor;
        }

        public String getNameForNavigation() {
            return nameForNavigation;
        }

        public String buildEntityUrl(String baseUrl, long spaceId, long workspaceId, String entityId) {
            //OctaneConfiguration octaneConfiguration = OctaneConfigurationManager.getInstance().getConfiguration();
            String octaneEntityUrl = String.format("%s/ui/?p=%s/%s#/entity-navigation?entityType=%s&id=%s",
                    baseUrl, spaceId, workspaceId,
                    this.getNameForNavigation(), entityId);
            return octaneEntityUrl;
        }
    }
}
