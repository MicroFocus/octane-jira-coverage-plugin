package com.microfocus.octane.plugins.views;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.google.gson.Gson;
import com.microfocus.octane.plugins.components.api.OctaneRestService;
import com.microfocus.octane.plugins.configuration.OctaneConfiguration;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;
import com.microfocus.octane.plugins.rest.entities.MapBasedObject;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntity;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;
import com.microfocus.octane.plugins.rest.query.InQueryPhrase;
import com.microfocus.octane.plugins.rest.query.LogicalQueryPhrase;
import com.microfocus.octane.plugins.rest.query.QueryPhrase;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Scanned
public class TestCoverageWebPanel extends AbstractJiraContextProvider {

    private OctaneRestService octaneRestService;
    private NumberFormat countFormat = NumberFormat.getInstance();
    private NumberFormat percentFormatter = NumberFormat.getPercentInstance();
    private Map<String, TypeDescriptor> typeDescriptors = new HashMap<>();
    private Map<String, TestStatusDescriptor> testStatusByNameDescriptors = new HashMap<>();
    private Map<String, TestStatusDescriptor> testStatusByLogicalNameDescriptors = new HashMap<>();

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
        TestStatusDescriptor plannedStatus = new TestStatusDescriptor("planned", "list_node.run_status.planned", "Planned", "rgb(47, 214, 195)", 3);
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
        OctaneConfiguration octaneConfiguration = OctaneConfigurationManager.getInstance().getConfiguration();
        Map<String, Object> contextMap = new HashMap<>();
        Issue currentIssue = (Issue) jiraHelper.getContextParams().get("issue");

        QueryPhrase jiraKeyCondition = new LogicalQueryPhrase(octaneConfiguration.getOctaneUdf(), currentIssue.getKey());

        //CHECK Application modules
        OctaneEntityCollection applicationModules = octaneRestService.getEntitiesByCondition("application_modules", Arrays.asList(jiraKeyCondition), Arrays.asList("path", "name"));
        List<MapBasedObject> groups = null;
        TypeDescriptor typeDescriptor = null;
        int total = 0;
        OctaneEntity octaneEntity = null;
        if (!applicationModules.getData().isEmpty()) {
            octaneEntity = applicationModules.getData().get(0);
            typeDescriptor = typeDescriptors.get(octaneEntity.getType());
            String path = octaneEntity.getString("path");

            GroupEntityCollection coverage = octaneRestService.getCoverageForApplicationModule(path);
            int myTotal = total = coverage.getGroups().stream().filter(gr -> gr.getValue() != null).mapToInt(o -> o.getCount()).sum();
            groups = coverage.getGroups().stream().filter(gr -> gr.getValue() != null).map(gr -> convertGroupEntityToUiEntity(gr, myTotal))
                    .sorted(Comparator.comparing(a -> (Integer) a.get("order"))).collect(Collectors.toList());

        } else {
            //CHECK WORKING ITEM
            QueryPhrase subTypeCondition = new InQueryPhrase("subtype", Arrays.asList("story", "feature"));
            OctaneEntityCollection workItems = octaneRestService.getEntitiesByCondition("work_items",
                    Arrays.asList(jiraKeyCondition, subTypeCondition), Arrays.asList("subtype", "name", "last_runs"));
            if (!workItems.getData().isEmpty()) {
                octaneEntity = workItems.getData().get(0);

                typeDescriptor = typeDescriptors.get(octaneEntity.getString("subtype"));
                String lastRuns = octaneEntity.getString("last_runs");
                Map<String, Double> name2countStatuses = new Gson().fromJson(lastRuns, Map.class);
                int myTotal = total = name2countStatuses.values().stream().mapToInt(a -> a.intValue()).sum();
                groups = name2countStatuses.entrySet().stream().filter(entry -> entry.getValue() > 0)
                        .map(entry -> convertGroupEntityToUiEntity(entry.getKey(), entry.getValue().intValue(), myTotal))
                        .sorted(Comparator.comparing(a -> (Integer) a.get("order"))).collect(Collectors.toList());
            }
        }

        if (octaneEntity != null) {
            contextMap.put("hasData", true);
            octaneEntity.put("url", typeDescriptor.buildEntityUrl(octaneConfiguration, octaneEntity.getId()));
            octaneEntity.put("typeKey", typeDescriptor.getTypeKey());
            octaneEntity.put("typeColor", typeDescriptor.getTypeColor());
            contextMap.put("total", total);
            contextMap.put("groups", groups);
            contextMap.put("hasData", true);
            contextMap.put("octaneEntity", octaneEntity);
        } else {
            contextMap.put("hasData", false);
        }

        return contextMap;
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

        public String buildEntityUrl(OctaneConfiguration octaneConfiguration, String entityId) {
            String octaneEntityUrl = String.format("%s/ui/?p=%s/%s#/entity-navigation?entityType=%s&id=%s",
                    octaneConfiguration.getBaseUrl(), octaneConfiguration.getSharedspaceId(), octaneConfiguration.getWorkspaceId(),
                    this.getNameForNavigation(), entityId);
            return octaneEntityUrl;
        }
    }
}
