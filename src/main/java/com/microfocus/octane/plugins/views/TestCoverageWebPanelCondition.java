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
import com.microfocus.octane.plugins.configuration.ConfigurationManager;
import com.microfocus.octane.plugins.configuration.v3.WorkspaceConfiguration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestCoverageWebPanelCondition implements Condition {

    ConfigurationManager configManager;

    public TestCoverageWebPanelCondition() {
        configManager = ConfigurationManager.getInstance();
    }

    @Override
    public void init(Map<String, String> map) throws PluginParseException {

    }

    @Override
    public boolean shouldDisplay(Map<String, Object> map) {
        Project project = (Project) map.get("project");

        List<WorkspaceConfiguration> workspaceConfigs = ConfigurationManager.getInstance().getWorkspaceConfigurations().stream()
                .filter(wc -> wc.getJiraConfigGrouping().getProjectNames().contains(project.getKey()))
                .collect(Collectors.toList());

        if (!workspaceConfigs.isEmpty()) {
            Issue issue = (Issue) map.get("issue");
            String issueType = issue.getIssueType().getName();

            return workspaceConfigs.stream().anyMatch(workspaceConfig -> workspaceConfig.getJiraConfigGrouping().getIssueTypes().contains(issueType));
        }

        return false;
    }
}
