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

package com.microfocus.octane.plugins.configuration.v2;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkspaceConfigurationV2 {

    private String id;
    private String spaceConfigurationId;

    private long workspaceId;
    private String workspaceName; //OctaneWorkspace
    private String octaneUdf;
    private Set<String> octaneEntityTypes; //OctaneConfiguration

    private Set<String> jiraIssueTypes;
    private Set<String> jiraProjects;

    public long getWorkspaceId() {
        return workspaceId;
    }

    public WorkspaceConfigurationV2 setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
        return this;
    }

    public String getOctaneUdf() {
        return octaneUdf;
    }

    public WorkspaceConfigurationV2 setOctaneUdf(String octaneUdf) {
        this.octaneUdf = octaneUdf;
        return this;
    }

    public Set<String> getOctaneEntityTypes() {
        return octaneEntityTypes;
    }

    public WorkspaceConfigurationV2 setOctaneEntityTypes(Set<String> octaneEntityTypes) {
        this.octaneEntityTypes = octaneEntityTypes;
        return this;
    }

    public Set<String> getJiraIssueTypes() {
        return jiraIssueTypes;
    }

    public WorkspaceConfigurationV2 setJiraIssueTypes(Set<String> jiraIssueTypes) {
        this.jiraIssueTypes = jiraIssueTypes;
        return this;
    }

    public Set<String> getJiraProjects() {
        return jiraProjects;
    }

    public WorkspaceConfigurationV2 setJiraProjects(Set<String> jiraProjects) {
        this.jiraProjects = jiraProjects;
        return this;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public WorkspaceConfigurationV2 setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
        return this;
    }

    public String getId() {
        return id;
    }

    public WorkspaceConfigurationV2 setId(String id) {
        this.id = id;
        return this;
    }

    public String getSpaceConfigurationId() {
        return spaceConfigurationId;
    }

    public WorkspaceConfigurationV2 setSpaceConfigurationId(String spaceConfigurationId) {
        this.spaceConfigurationId = spaceConfigurationId;
        return this;
    }
}
