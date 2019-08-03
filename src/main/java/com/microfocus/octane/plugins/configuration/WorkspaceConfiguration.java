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

package com.microfocus.octane.plugins.configuration;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkspaceConfiguration {

    private String id;
    private String spaceConfigurationId;
    private long workspaceId;
    private String workspaceName;
    private String octaneUdf;
    private List<String> octaneEntityTypes;
    private List<String> jiraIssueTypes;
    private List<String> jiraProjects;

    public long getWorkspaceId() {
        return workspaceId;
    }

    public WorkspaceConfiguration setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
        return this;
    }

    public String getOctaneUdf() {
        return octaneUdf;
    }

    public WorkspaceConfiguration setOctaneUdf(String octaneUdf) {
        this.octaneUdf = octaneUdf;
        return this;
    }

    public List<String> getOctaneEntityTypes() {
        return octaneEntityTypes;
    }

    public WorkspaceConfiguration setOctaneEntityTypes(List<String> octaneEntityTypes) {
        this.octaneEntityTypes = octaneEntityTypes;
        return this;
    }

    public List<String> getJiraIssueTypes() {
        return jiraIssueTypes;
    }

    public WorkspaceConfiguration setJiraIssueTypes(List<String> jiraIssueTypes) {
        this.jiraIssueTypes = jiraIssueTypes;
        return this;
    }

    public List<String> getJiraProjects() {
        return jiraProjects;
    }

    public WorkspaceConfiguration setJiraProjects(List<String> jiraProjects) {
        this.jiraProjects = jiraProjects;
        return this;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public WorkspaceConfiguration setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
        return this;
    }

    public String getId() {
        return id;
    }

    public WorkspaceConfiguration setId(String id) {
        this.id = id;
        return this;
    }

    public String getSpaceConfigurationId() {
        return spaceConfigurationId;
    }

    public WorkspaceConfiguration setSpaceConfigurationId(String spaceConfigurationId) {
        this.spaceConfigurationId = spaceConfigurationId;
        return this;
    }
}
