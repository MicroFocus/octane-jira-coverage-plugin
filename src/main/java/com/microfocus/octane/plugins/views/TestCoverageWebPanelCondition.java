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
