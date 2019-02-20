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
import com.microfocus.octane.plugins.components.api.OctaneRestService;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;
import com.microfocus.octane.plugins.configuration.WorkspaceConfiguration;
import com.microfocus.octane.plugins.descriptors.AggregateDescriptor;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeDescriptor;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeManager;
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

    private Map<String, TestStatusDescriptor> testStatusByLogicalNameDescriptors = new HashMap<>();
    private final static String UDF_NOT_DEFINED_IN_OCTANE = "platform.unknown_field";

    public TestCoverageWebPanel(OctaneRestService octaneRestService) {
        this.octaneRestService = octaneRestService;
        percentFormatter.setMinimumFractionDigits(1);
        percentFormatter.setMinimumFractionDigits(1);


        //TEST TYPES
        TestStatusDescriptor passedStatus = new TestStatusDescriptor("passed", "list_node.run_status.passed", "Passed", "rgb(26, 172, 96)", 1);
        TestStatusDescriptor failedStatus = new TestStatusDescriptor("failed", "list_node.run_status.failed", "Failed", "rgb(229, 0, 76)", 2);
        TestStatusDescriptor plannedStatus = new TestStatusDescriptor("planned", "list_node.run_status.planned", "Planned", "#dddddd" /*"rgb(47, 214, 195)"*/, 3);
        TestStatusDescriptor skippedStatus = new TestStatusDescriptor("skipped", "list_node.run_status.skipped", "Skipped", "rgb(82, 22, 172)", 4);
        TestStatusDescriptor needAttentionStatus = new TestStatusDescriptor("needsAttention", "list_node.run_status.requires_attention", "Requires Attention", "rgb(252, 219, 31)", 5);

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
                for (AggregateDescriptor aggDescriptor : OctaneEntityTypeManager.getAggregators()) {
                    boolean isMatch = false;
                    for (OctaneEntityTypeDescriptor entityDesc : aggDescriptor.getDescriptors()) {
                        if (workspaceConfig.getOctaneEntityTypes().contains(entityDesc.getTypeName())) {
                            isMatch = true;
                            break;
                        }
                    }
                    if (isMatch) {
                        found = tryGetCoverageForDescriptor(workspaceConfig, contextMap, jiraKeyCondition, aggDescriptor);
                        if (found) {
                            break;
                        }
                    }
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

    private boolean tryGetCoverageForDescriptor(WorkspaceConfiguration workspaceConfiguration, Map<String, Object> contextMap, QueryPhrase jiraKeyCondition, AggregateDescriptor aggrDescriptor) {
        try {
            boolean subtypedEntity = false;
            List<QueryPhrase> conditions = new ArrayList<>();
            conditions.add(jiraKeyCondition);

            List<String> fields = new ArrayList<>();
            fields.add("name");
            if (aggrDescriptor.getDescriptors().size() > 1) {
                fields.add("subtype");
                List<String> typeNames = aggrDescriptor.getDescriptors().stream().map(OctaneEntityTypeDescriptor::getTypeName).collect(Collectors.toList());
                QueryPhrase subTypeCondition = new InQueryPhrase("subtype", typeNames);
                conditions.add(subTypeCondition);
                subtypedEntity = true;
            } else {
                if (aggrDescriptor.getDescriptors().get(0).isHierarchicalEntity()) {
                    fields.add("path");
                }
            }
            OctaneEntityCollection entities = octaneRestService.getEntitiesByCondition(workspaceConfiguration.getWorkspaceId(), aggrDescriptor.getCollectionName(), Arrays.asList(jiraKeyCondition), fields);
            if (!entities.getData().isEmpty()) {
                OctaneEntity octaneEntity = entities.getData().get(0);
                OctaneEntityTypeDescriptor typeDescriptor;
                if (subtypedEntity) {
                    typeDescriptor = OctaneEntityTypeManager.getByTypeName(octaneEntity.getString("subtype"));
                } else {
                    typeDescriptor = OctaneEntityTypeManager.getByTypeName(octaneEntity.getType());
                }

                getCoverageAndFillContextMap(octaneEntity, typeDescriptor, workspaceConfiguration, contextMap);
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

    private void getCoverageAndFillContextMap(OctaneEntity octaneEntity, OctaneEntityTypeDescriptor typeDescriptor, WorkspaceConfiguration workspaceConfiguration, Map<String, Object> contextMap) {
        GroupEntityCollection coverage = octaneRestService.getCoverage(octaneEntity, typeDescriptor, workspaceConfiguration.getWorkspaceId());

        int total = coverage.getGroups().stream().filter(gr -> gr.getValue() != null).mapToInt(o -> o.getCount()).sum();
        List<MapBasedObject> groups = coverage.getGroups().stream().filter(gr -> gr.getValue() != null).map(gr -> convertGroupEntityToUiEntity(gr, total))
                .sorted(Comparator.comparing(a -> (Integer) a.get("order"))).collect(Collectors.toList());

        octaneEntity.put("url", typeDescriptor.buildEntityUrl(workspaceConfiguration.getSpaceConfiguration().getLocationParts().getBaseUrl(),
                workspaceConfiguration.getSpaceConfiguration().getLocationParts().getSpaceId(), workspaceConfiguration.getWorkspaceId(), octaneEntity.getId()));
        octaneEntity.put("testTabUrl", typeDescriptor.buildTestTabEntityUrl(workspaceConfiguration.getSpaceConfiguration().getLocationParts().getBaseUrl(),
                workspaceConfiguration.getSpaceConfiguration().getLocationParts().getSpaceId(), workspaceConfiguration.getWorkspaceId(), octaneEntity.getId()));
        octaneEntity.put("typeAbbreviation", typeDescriptor.getTypeAbbreviation());
        octaneEntity.put("typeColor", typeDescriptor.getTypeColor());
        contextMap.put("total", total);
        contextMap.put("groups", groups);
        contextMap.put("status", "hasData");
        contextMap.put("octaneEntity", octaneEntity);
        contextMap.put("hasData", true);
    }

    private MapBasedObject convertGroupEntityToUiEntity(GroupEntity groupEntity, int totalCount) {
        TestStatusDescriptor testStatusDescriptor = testStatusByLogicalNameDescriptors.get(groupEntity.getValue().getId());

        MapBasedObject outputEntity = new MapBasedObject();
        outputEntity.put("color", testStatusDescriptor.getColor());
        outputEntity.put("order", testStatusDescriptor.getOrder());
        outputEntity.put("count", countFormat.format(groupEntity.getCount()));
        outputEntity.put("name", testStatusDescriptor.getTitle());

        float percentage = 1f * groupEntity.getCount() / totalCount;
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
}
