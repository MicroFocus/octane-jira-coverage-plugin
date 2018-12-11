package com.microfocus.octane.plugins.views;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.microfocus.octane.plugins.components.api.OctaneRestService;
import com.microfocus.octane.plugins.configuration.OctaneConfiguration;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntity;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;
import com.microfocus.octane.plugins.rest.query.LogicalQueryPhrase;
import com.microfocus.octane.plugins.rest.query.QueryPhrase;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Scanned
public class TestCoverageWebPanel extends AbstractJiraContextProvider {

    private OctaneRestService octaneRestService;
    private PluginSettingsFactory pluginSettingsFactory;
    private NumberFormat countFormat = NumberFormat.getInstance();
    private NumberFormat percentFormatter = NumberFormat.getPercentInstance();

    public TestCoverageWebPanel(OctaneRestService octaneRestService, PluginSettingsFactory pluginSettingsFactory) {

        this.octaneRestService = octaneRestService;
        this.pluginSettingsFactory = pluginSettingsFactory;

        percentFormatter.setMinimumFractionDigits(1);
        percentFormatter.setMinimumFractionDigits(1);
    }

    @Override
    public Map<String, Object> getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        OctaneConfiguration octaneConfiguration = OctaneConfigurationManager.loadConfiguration(pluginSettingsFactory);
        Map<String, Object> contextMap = new HashMap<>();
        Issue currentIssue = (Issue) jiraHelper.getContextParams().get("issue");


        QueryPhrase condition = new LogicalQueryPhrase(octaneConfiguration.getOctaneUdf(), currentIssue.getKey());
        OctaneEntityCollection collection = octaneRestService.getEntitiesByCondition("application_modules", condition);
        if (!collection.getData().isEmpty()) {
            OctaneEntity entity = collection.getData().get(0);
            String path = entity.getString("path");
            List<OctaneEntity> groups = new ArrayList<>();

            GroupEntityCollection coverage = octaneRestService.getCoverage(path);
            Map<String, GroupEntity> id2entity = coverage.getGroups().stream().filter(gr -> gr.getValue() != null).collect(Collectors.toMap(gr -> gr.getValue().getId(), Function.identity()));

            int total = id2entity.values().stream().mapToInt(o -> o.getCount()).sum();
            extractAndEnrichEntity(groups, id2entity.get("list_node.run_status.passed"), "rgb(26, 172, 96)", total);
            extractAndEnrichEntity(groups, id2entity.get("list_node.run_status.failed"), "rgb(229, 0, 76)", total);
            extractAndEnrichEntity(groups, id2entity.get("list_node.run_status.planned"), "rgb(47, 214, 195)", total);
            extractAndEnrichEntity(groups, id2entity.get("list_node.run_status.skipped"), "rgb(82, 22, 172)", total);
            extractAndEnrichEntity(groups, id2entity.get("list_node.run_status.requires_attention"), "rgb(252, 219, 31)", total);

            String octaneEntityUrl = String.format("%s/ui/?p=%s/%s#/entity-navigation?entityType=%s&id=%s",
                    octaneConfiguration.getBaseUrl(), octaneConfiguration.getSharespaceId(), octaneConfiguration.getWorkspaceId(),
                    "product_area", entity.getId());

            contextMap.put("total", total);
            contextMap.put("groups", groups);
            contextMap.put("octaneEntityUrl", octaneEntityUrl);
            contextMap.put("hasData", true);
            contextMap.put("octaneEntityId", entity.getId());
            contextMap.put("octaneEntityName", entity.getName());
        } else {
            contextMap.put("hasData", false);
        }


        return contextMap;

    }

    private void extractAndEnrichEntity(List<OctaneEntity> result, GroupEntity groupEntity, String color, int totalCount) {
        if (groupEntity != null) {
            OctaneEntity entity = groupEntity.getValue();
            entity.put("color", color);

            entity.put("count", countFormat.format(groupEntity.getCount()));

            float percentage = 1f * groupEntity.getCount() / totalCount;
            String percentageAsString = percentFormatter.format(percentage);//String.format("%.1f", percentage);
            entity.put("percentage", percentageAsString);
            result.add(entity);
        }
    }
}
