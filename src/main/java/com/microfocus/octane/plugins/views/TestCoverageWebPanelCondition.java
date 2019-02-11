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
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;
import com.microfocus.octane.plugins.configuration.SpaceConfiguration;
import com.microfocus.octane.plugins.configuration.WorkspaceConfiguration;

import java.util.Map;
import java.util.Optional;

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
            SpaceConfiguration spaceConfiguration = configManager.getConfiguration();
            Project project = (Project) map.get("project");

            Optional<WorkspaceConfiguration> workspaceConfigOpt = spaceConfiguration.getWorkspaces().stream().filter(w -> w.getJiraProjects().contains(project.getName())).findFirst();
            if (workspaceConfigOpt.isPresent()) {
                Issue issue = (Issue) map.get("issue");
                String issueType = issue.getIssueType().getName();
                WorkspaceConfiguration workspaceConfig = workspaceConfigOpt.get();
                if (workspaceConfig.getJiraIssueTypes().isEmpty() || workspaceConfig.getJiraIssueTypes().contains(issueType)) {
                    return true;
                }
            }
        }

        return false;
    }
}
