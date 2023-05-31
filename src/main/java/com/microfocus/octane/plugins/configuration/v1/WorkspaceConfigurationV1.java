/*******************************************************************************
 * Copyright 2017-2023 Open Text.
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.microfocus.octane.plugins.configuration.v1;

import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.Set;


public class WorkspaceConfigurationV1 {

    private long workspaceId;
    private String workspaceName;
    private String octaneUdf;
    private Set<String> octaneEntityTypes;
    private Set<String> jiraIssueTypes;
    private Set<String> jiraProjects;

    @JsonIgnore
    private SpaceConfigurationV1 spaceConfiguration;

    public long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getOctaneUdf() {
        return octaneUdf;
    }

    public void setOctaneUdf(String octaneUdf) {
        this.octaneUdf = octaneUdf;
    }

    public Set<String> getOctaneEntityTypes() {
        return octaneEntityTypes;
    }

    public void setOctaneEntityTypes(Set<String> octaneEntityTypes) {
        this.octaneEntityTypes = octaneEntityTypes;
    }

    public Set<String> getJiraIssueTypes() {
        return jiraIssueTypes;
    }

    public void setJiraIssueTypes(Set<String> jiraIssueTypes) {
        this.jiraIssueTypes = jiraIssueTypes;
    }

    public Set<String> getJiraProjects() {
        return jiraProjects;
    }

    public void setJiraProjects(Set<String> jiraProjects) {
        this.jiraProjects = jiraProjects;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    @JsonIgnore
    public SpaceConfigurationV1 getSpaceConfiguration() {
        return spaceConfiguration;
    }

    public void setSpaceConfiguration(SpaceConfigurationV1 spaceConfiguration) {
        this.spaceConfiguration = spaceConfiguration;
    }
}
