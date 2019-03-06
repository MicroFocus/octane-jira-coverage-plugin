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
import com.atlassian.jira.user.ApplicationUser;
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
import java.util.function.Function;
import java.util.stream.Collectors;

public class CoverageUiHelper {

    private static Map<String, TestStatusDescriptor> testStatusDescriptors = new HashMap<>();
    private static NumberFormat countFormat = NumberFormat.getInstance();
    private static NumberFormat percentFormatter = NumberFormat.getPercentInstance();
    private static OctaneConfigurationManager configurationManager = OctaneConfigurationManager.getInstance();
    private final static String UDF_NOT_DEFINED_IN_OCTANE = "platform.unknown_field";
    private static final Logger log = LoggerFactory.getLogger(CoverageUiHelper.class);

    static {

        percentFormatter.setMinimumFractionDigits(1);
        percentFormatter.setMinimumFractionDigits(1);

        //TEST TYPES
        TestStatusDescriptor passedStatus = new TestStatusDescriptor("list_node.run_status.passed", "run_status_passed", "Passed", "#1aac60", 1);
        TestStatusDescriptor failedStatus = new TestStatusDescriptor("list_node.run_status.failed", "run_status_failed", "Failed", "#e5004c", 2);
        TestStatusDescriptor plannedStatus = new TestStatusDescriptor("list_node.run_status.planned", "run_status_planned", "Planned", "#dddddd", 3);
        TestStatusDescriptor skippedStatus = new TestStatusDescriptor("list_node.run_status.skipped", "run_status_skipped", "Skipped", "#5216ac", 4);
        TestStatusDescriptor needAttentionStatus = new TestStatusDescriptor("list_node.run_status.requires_attention", "run_status_requires_attention", "Requires Attention", "#fcdb1f", 5);

        //BY LOGICAL NAME
        testStatusDescriptors.put(passedStatus.getLogicalName(), passedStatus);
        testStatusDescriptors.put(failedStatus.getLogicalName(), failedStatus);
        testStatusDescriptors.put(plannedStatus.getLogicalName(), plannedStatus);
        testStatusDescriptors.put(skippedStatus.getLogicalName(), skippedStatus);
        testStatusDescriptors.put(needAttentionStatus.getLogicalName(), needAttentionStatus);
    }

    public static List<MapBasedObject> getAllCoverageGroups() {
        List<MapBasedObject> groups = testStatusDescriptors.keySet().stream()
                .map(key -> convertGroupEntityToUiEntity(testStatusDescriptors.get(key), 0, 0))
                .sorted(Comparator.comparing(a -> (Integer) a.get("order")))
                .collect(Collectors.toList());
        return groups;
    }

    private static MapBasedObject convertGroupEntityToUiEntity(TestStatusDescriptor testStatusDescriptor, int groupCount, int totalCount) {
        MapBasedObject outputEntity = new MapBasedObject();
        outputEntity.put("color", testStatusDescriptor.getColor());
        outputEntity.put("order", testStatusDescriptor.getOrder());
        outputEntity.put("count", groupCount);
        outputEntity.put("countStr", countFormat.format(groupCount));
        outputEntity.put("name", testStatusDescriptor.getTitle());
        outputEntity.put("id", testStatusDescriptor.getKey());

        float percentage = (totalCount == 0) ? 0f : 1f * groupCount / totalCount;
        String percentageAsString = percentFormatter.format(percentage);//String.format("%.1f", percentage);
        outputEntity.put("percentage", percentageAsString);
        return outputEntity;
    }

    private static List<MapBasedObject> getCoverageGroups(OctaneRestService octaneRestService, OctaneEntity octaneEntity, OctaneEntityTypeDescriptor typeDescriptor, long workspaceId) {
        GroupEntityCollection coverage = octaneRestService.getCoverage(octaneEntity, typeDescriptor, workspaceId, null);
        Map<String, GroupEntity> id2group = coverage.getGroups().stream().filter(gr -> gr.getValue() != null).collect(Collectors.toMap(g -> ((OctaneEntity) g.getValue()).getId(), Function.identity()));
        int total = id2group.values().stream().mapToInt(o -> o.getCount()).sum();
        List<MapBasedObject> groups = id2group.entrySet().stream()
                .map(entry -> convertGroupEntityToUiEntity(testStatusDescriptors.get(entry.getKey()), entry.getValue().getCount(), total))
                .sorted(Comparator.comparing(a -> (Integer) a.get("order")))
                .collect(Collectors.toList());

        return groups;
    }

    private static OctaneEntity tryFindMatchingOctaneEntity(OctaneRestService octaneRestService, WorkspaceConfiguration workspaceConfiguration, QueryPhrase jiraKeyCondition, AggregateDescriptor aggrDescriptor) {
        try {
            List<QueryPhrase> conditions = new ArrayList<>();
            conditions.add(jiraKeyCondition);

            List<String> fields = new ArrayList<>();
            fields.add("name");
            fields.add("path");
            if (aggrDescriptor.isSubtyped()) {
                fields.add("subtype");
                List<String> typeNames = aggrDescriptor.getDescriptors().stream().map(OctaneEntityTypeDescriptor::getTypeName).collect(Collectors.toList());
                QueryPhrase subTypeCondition = new InQueryPhrase("subtype", typeNames);
                conditions.add(subTypeCondition);
            }

            OctaneEntityCollection entities = octaneRestService.getEntitiesByCondition(workspaceConfiguration.getWorkspaceId(), aggrDescriptor.getCollectionName(), conditions, fields);
            if (!entities.getData().isEmpty()) {
                OctaneEntity octaneEntity = entities.getData().get(0);
                return octaneEntity;
            }
        } catch (RestStatusException e) {
            if (UDF_NOT_DEFINED_IN_OCTANE.equals(e.getErrorCode())) {
                //field is not defined - skip
            } else {
                throw e;
            }
        }
        return null;
    }

    public static Map<String, Object> buildCoverageContextMap(OctaneRestService octaneRestService, String projectKey, String issueKey, String issueId) {

        Map<String, Object> contextMap = new HashMap<>();
        Map<String, Object> debugMap = new HashMap<>();
        LinkedHashMap<String, Long> perfMap = new LinkedHashMap<>();
        contextMap.put("issueKey", issueKey);

        long startTotal = System.currentTimeMillis();
        if (configurationManager.isValidConfiguration()) {
            try {
                WorkspaceConfiguration workspaceConfig = configurationManager.getConfiguration().getWorkspaces().stream()
                        .filter(w -> w.getJiraProjects().contains(projectKey)).findFirst().get();//we use get without validation because validation is done in condition

                QueryPhrase jiraKeyCondition = new InQueryPhrase(workspaceConfig.getOctaneUdf(), Arrays.asList(issueKey, issueId.toString()));
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
                        long start = System.currentTimeMillis();
                        OctaneEntity octaneEntity = tryFindMatchingOctaneEntity(octaneRestService, workspaceConfig, jiraKeyCondition, aggDescriptor);
                        long duration = System.currentTimeMillis() - start;
                        perfMap.put(aggDescriptor.getCollectionName(), duration);
                        if (octaneEntity != null) {
                            OctaneEntityTypeDescriptor typeDescriptor = (aggDescriptor.isSubtyped() ? OctaneEntityTypeManager.getByTypeName(octaneEntity.getString("subtype")) :
                                    OctaneEntityTypeManager.getByTypeName(octaneEntity.getType()));

                            //totalTestsCount
                            start = System.currentTimeMillis();
                            long totalTestCount = octaneRestService.getTotalTestsCount(octaneEntity, typeDescriptor, workspaceConfig.getWorkspaceId());
                            perfMap.put("totalTestsCount", System.currentTimeMillis() - start);

                            //coverage
                            start = System.currentTimeMillis();
                            List<MapBasedObject> coverageGroups = getCoverageGroups(octaneRestService, octaneEntity, typeDescriptor, workspaceConfig.getWorkspaceId());
                            perfMap.put("coverage", System.currentTimeMillis() - start);

                            //fill context map
                            octaneEntity.put("url", typeDescriptor.buildEntityUrl(workspaceConfig.getSpaceConfiguration().getLocationParts().getBaseUrl(),
                                    workspaceConfig.getSpaceConfiguration().getLocationParts().getSpaceId(), workspaceConfig.getWorkspaceId(), octaneEntity.getId()));
                            octaneEntity.put("testTabUrl", typeDescriptor.buildTestTabEntityUrl(workspaceConfig.getSpaceConfiguration().getLocationParts().getBaseUrl(),
                                    workspaceConfig.getSpaceConfiguration().getLocationParts().getSpaceId(), workspaceConfig.getWorkspaceId(), octaneEntity.getId()));
                            octaneEntity.put("typeAbbreviation", typeDescriptor.getTypeAbbreviation());
                            octaneEntity.put("typeColor", typeDescriptor.getTypeColor());
                            contextMap.put("octaneEntity", octaneEntity);
                            contextMap.put("totalExecutedTestsCount", coverageGroups.stream().mapToInt(g -> (Integer) g.get("count")).sum());
                            contextMap.put("coverageGroups", coverageGroups);
                            contextMap.put("totalTestsCount", totalTestCount);
                            //contextMap.put("atl.gh.issue.details.tab.count", total);
                            contextMap.put("status", "hasData");
                            found = true;
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
                    log.error("Failed to fill ContextMap (RestStatusException) : " + e.getMessage());
                }
                debugMap.put("error", String.format("RestStatusException %s, Error : %s ", e.getResponse().getStatusCode(), e.getMessage()));
            } catch (Exception e) {
                log.error(String.format("Failed to fill ContextMap (%s) : %s", e.getClass().getName(), e.getMessage()));
                debugMap.put("error", String.format("Error %s : %s %s", e.getClass().getName(), e.getMessage(), e.getCause() != null ? ", cause : " + e.getCause().getMessage() : ""));
            }
        }

        if (!contextMap.containsKey("status")) {
            contextMap.put("status", "noValidConfiguration");
        }

        ApplicationUser appuser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        boolean showDebug = (boolean) OctaneConfigurationManager.getInstance().getUserParameter(appuser.getUsername(), OctaneConfigurationManager.SHOW_DEBUG_PARAMETER, false);
        if (showDebug) {
            perfMap.put("total", System.currentTimeMillis() - startTotal);
            debugMap.put("perf", perfMap.entrySet().stream().map(entry -> entry.getKey() + " " + entry.getValue()).collect(Collectors.joining("; ")));
            contextMap.put("debug", debugMap);
        }

        return contextMap;
    }

}
