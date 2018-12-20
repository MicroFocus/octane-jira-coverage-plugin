package com.microfocus.octane.plugins.views;


import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.microfocus.octane.plugins.configuration.OctaneConfiguration;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;

import java.util.Map;

public class TestCoverageWebPanelCondition implements Condition {

    OctaneConfigurationManager configManager;

    public TestCoverageWebPanelCondition() {
        configManager = OctaneConfigurationManager.getInstance();
    }

    @Override
    public void init(Map<String, String> map) throws PluginParseException {

    }

    @Override
    public boolean shouldDisplay(Map<String, Object> map) {
        if (configManager.isValidConfiguration()) {
            OctaneConfiguration octaneConfiguration = configManager.getConfiguration();
            if (!octaneConfiguration.getJiraProjects().isEmpty()) {
                Project project = (Project) map.get("project");
                String projectKey = project.getKey().toUpperCase();
                if (!octaneConfiguration.getJiraProjects().contains(projectKey)) {
                    return false;
                }
            }

            if (!octaneConfiguration.getJiraIssueTypes().isEmpty()) {
                Issue issue = (Issue) map.get("issue");
                String issueType = issue.getIssueType().getName().toLowerCase();
                if (!octaneConfiguration.getJiraIssueTypes().contains(issueType)) {
                    return false;
                }
            }
        }

        return true;
    }
}
