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
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeDescriptor;
import com.microfocus.octane.plugins.rest.entities.MapBasedObject;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntity;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CoverageUiHelper {

    private static Map<String, TestStatusDescriptor> testStatusDescriptors = new HashMap<>();
    private static NumberFormat countFormat = NumberFormat.getInstance();
    private static NumberFormat percentFormatter = NumberFormat.getPercentInstance();

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

    public static void getCoverageAndFillContextMap(OctaneRestService octaneRestService, OctaneEntity octaneEntity, OctaneEntityTypeDescriptor typeDescriptor, WorkspaceConfiguration workspaceConfiguration, Map<String, Object> contextMap) {

        ApplicationUser appuser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

        String lastRunStartedFilter = OctaneConfigurationManager.getInstance().getUserFilter(appuser.getUsername());
        //runs
        List<MapBasedObject> coverageGroups = getCoverageGroups(octaneRestService, octaneEntity, typeDescriptor, workspaceConfiguration.getWorkspaceId(), lastRunStartedFilter);

        octaneEntity.put("url", typeDescriptor.buildEntityUrl(workspaceConfiguration.getSpaceConfiguration().getLocationParts().getBaseUrl(),
                workspaceConfiguration.getSpaceConfiguration().getLocationParts().getSpaceId(), workspaceConfiguration.getWorkspaceId(), octaneEntity.getId()));
        octaneEntity.put("testTabUrl", typeDescriptor.buildTestTabEntityUrl(workspaceConfiguration.getSpaceConfiguration().getLocationParts().getBaseUrl(),
                workspaceConfiguration.getSpaceConfiguration().getLocationParts().getSpaceId(), workspaceConfiguration.getWorkspaceId(), octaneEntity.getId()));
        octaneEntity.put("typeAbbreviation", typeDescriptor.getTypeAbbreviation());
        octaneEntity.put("typeColor", typeDescriptor.getTypeColor());
        contextMap.put("totalRuns", coverageGroups.stream().mapToInt(g -> (Integer) g.get("countInt")).sum());
        contextMap.put("runGroups", coverageGroups);
        contextMap.put("status", "hasData");
        contextMap.put("octaneEntity", octaneEntity);
        contextMap.put("lastRunStartedFilter", lastRunStartedFilter);
        contextMap.put("hasData", true);
        contextMap.put("filterQueryString", String.format("entity-id=%s&entity-path=%s&entity-type=%s&workspace-id=%s",
                octaneEntity.getId(), octaneEntity.isFieldSetAndNotEmpty("path") ? octaneEntity.get("path") : "",
                typeDescriptor.getTypeName(),
                workspaceConfiguration.getWorkspaceId()));

        //tests
        GroupEntityCollection allTests = octaneRestService.getAllTestsBySubtype(octaneEntity, typeDescriptor, workspaceConfiguration.getWorkspaceId());
        contextMap.put("totalTests", allTests.getGroups().stream().mapToInt(GroupEntity::getCount).sum());
    }

    private static MapBasedObject convertGroupEntityToUiEntity(TestStatusDescriptor testStatusDescriptor, int groupCount, int totalCount) {
        MapBasedObject outputEntity = new MapBasedObject();
        outputEntity.put("color", testStatusDescriptor.getColor());
        outputEntity.put("order", testStatusDescriptor.getOrder());
        outputEntity.put("countStr", countFormat.format(groupCount));
        outputEntity.put("countInt", groupCount);
        outputEntity.put("className", groupCount == 0 ? "hidden" : "");
        outputEntity.put("name", testStatusDescriptor.getTitle());
        outputEntity.put("id", testStatusDescriptor.getKey());

        float percentage = 1f * groupCount / totalCount;
        String percentageAsString = percentFormatter.format(percentage);//String.format("%.1f", percentage);
        outputEntity.put("percentage", percentageAsString);
        return outputEntity;
    }

    public static List<MapBasedObject> getCoverageGroups(OctaneRestService octaneRestService, OctaneEntity octaneEntity, OctaneEntityTypeDescriptor typeDescriptor, long workspaceId, String lastRunStartedFilter) {
        GroupEntityCollection coverage = octaneRestService.getCoverage(octaneEntity, typeDescriptor, workspaceId, lastRunStartedFilter);
        Map<String, GroupEntity> id2group = coverage.getGroups().stream().filter(gr -> gr.getValue() != null).collect(Collectors.toMap(g -> ((OctaneEntity) g.getValue()).getId(), Function.identity()));
        int total = id2group.values().stream().mapToInt(o -> o.getCount()).sum();
        List<MapBasedObject> groups = testStatusDescriptors.keySet().stream()
                .map(key -> convertGroupEntityToUiEntity(testStatusDescriptors.get(key), id2group.containsKey(key) ? id2group.get(key).getCount() : 0, total))
                .sorted(Comparator.comparing(a -> (Integer) a.get("order")))
                .collect(Collectors.toList());
        return groups;
    }
}
