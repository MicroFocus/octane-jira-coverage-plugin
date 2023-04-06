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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Scanned
public class TestCoverageWebPanel extends AbstractJiraContextProvider {

    private static final Logger log = LoggerFactory.getLogger(TestCoverageWebPanel.class);

    @Override
    public Map<String, Object> getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {

        Map<String, Object> contextMap = new HashMap<>();
        String configUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + "/plugins/servlet/admin/octane";
        contextMap.put("configUrl", configUrl);
        contextMap.put("runGroups", CoverageUiHelper.getAllCoverageGroups());

        Issue issue = (Issue) jiraHelper.getContextParams().get("issue");
        contextMap.put("issueKey", issue.getKey());
        contextMap.put("issueId", issue.getId());
        contextMap.put("issueType", issue.getIssueType().getName());
        contextMap.put("projectKey", jiraHelper.getProject().getKey());

        return contextMap;
    }

}
