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

import java.util.List;
import java.util.Set;


public class WorkspaceConfiguration {

    private String workspaceId;
    private String octaneUdf;
    private Set<String> octaneTypes;
    private Set<String> jiraIssueTypes;
    private Set<String> jiraProjects;


    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getOctaneUdf() {
        return octaneUdf;
    }

    public void setOctaneUdf(String octaneUdf) {
        this.octaneUdf = octaneUdf;
    }

    public Set<String> getOctaneTypes() {
        return octaneTypes;
    }

    public void setOctaneTypes(Set<String> octaneTypes) {
        this.octaneTypes = octaneTypes;
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
}
