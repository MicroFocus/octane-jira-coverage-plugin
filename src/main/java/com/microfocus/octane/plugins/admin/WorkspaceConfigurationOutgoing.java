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

package com.microfocus.octane.plugins.admin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkspaceConfigurationOutgoing {

    @XmlElement(name = "id")
    private String id;

    @XmlElement(name = "spaceConfigId")
    private String spaceConfigId;

    @XmlElement(name = "spaceConfigName")
    private String spaceConfigName;

    @XmlElement(name = "workspaceId")
    private String workspaceId;

    @XmlElement(name = "workspaceName")
    private String workspaceName;

    @XmlElement(name = "octaneUdf")
    private String octaneUdf;

    @XmlElement(name = "octaneEntityTypes")
    private Set<String> octaneEntityTypes;

    @XmlElement(name = "jiraIssueTypes")
    private Set<String> jiraIssueTypes;

    @XmlElement(name = "jiraProjects")
    private Set<String> jiraProjects;

    public String getId() {
        return id;
    }

    public WorkspaceConfigurationOutgoing setId(String id) {
        this.id = id;
        return this;
    }

    public String getOctaneUdf() {
        return octaneUdf;
    }

    public WorkspaceConfigurationOutgoing setOctaneUdf(String octaneUdf) {
        this.octaneUdf = octaneUdf;
        return this;
    }

    public Set<String> getOctaneEntityTypes() {
        return octaneEntityTypes;
    }

    public WorkspaceConfigurationOutgoing setOctaneEntityTypes(Set<String> octaneEntityTypes) {
        this.octaneEntityTypes = octaneEntityTypes;
        return this;
    }

    public Set<String> getJiraIssueTypes() {
        return jiraIssueTypes;
    }

    public WorkspaceConfigurationOutgoing setJiraIssueTypes(Set<String> jiraIssueTypes) {
        this.jiraIssueTypes = jiraIssueTypes;
        return this;
    }

    public Set<String> getJiraProjects() {
        return jiraProjects;
    }

    public WorkspaceConfigurationOutgoing setJiraProjects(Set<String> jiraProjects) {
        this.jiraProjects = jiraProjects;
        return this;
    }


    public String getSpaceConfigId() {
        return spaceConfigId;
    }

    public WorkspaceConfigurationOutgoing setSpaceConfigId(String spaceConfigId) {
        this.spaceConfigId = spaceConfigId;
        return this;
    }

    public String getSpaceConfigName() {
        return spaceConfigName;
    }

    public WorkspaceConfigurationOutgoing setSpaceConfigName(String spaceConfigName) {
        this.spaceConfigName = spaceConfigName;
        return this;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public WorkspaceConfigurationOutgoing setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
        return this;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public WorkspaceConfigurationOutgoing setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
        return this;
    }
}
