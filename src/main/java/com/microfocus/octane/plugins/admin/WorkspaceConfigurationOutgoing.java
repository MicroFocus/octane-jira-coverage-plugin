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
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkspaceConfigurationOutgoing {

    @XmlElement(name = "id")
    private String id;

    @XmlElement(name = "spaceConfig")
    private KeyValueItem spaceConfig;

    @XmlElement(name = "workspace")
    private KeyValueItem workspace;

    @XmlElement(name = "octaneUdf")
    private String octaneUdf;

    @XmlElement(name = "octaneEntityTypes")
    private List<String> octaneEntityTypes;

    @XmlElement(name = "jiraIssueTypes")
    private List<String> jiraIssueTypes;

    @XmlElement(name = "jiraProjects")
    private List<String> jiraProjects;

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

    public List<String> getOctaneEntityTypes() {
        return octaneEntityTypes;
    }

    public WorkspaceConfigurationOutgoing setOctaneEntityTypes(List<String> octaneEntityTypes) {
        this.octaneEntityTypes = octaneEntityTypes;
        return this;
    }

    public List<String> getJiraIssueTypes() {
        return jiraIssueTypes;
    }

    public WorkspaceConfigurationOutgoing setJiraIssueTypes(List<String> jiraIssueTypes) {
        this.jiraIssueTypes = jiraIssueTypes;
        return this;
    }

    public List<String> getJiraProjects() {
        return jiraProjects;
    }

    public WorkspaceConfigurationOutgoing setJiraProjects(List<String> jiraProjects) {
        this.jiraProjects = jiraProjects;
        return this;
    }


    public KeyValueItem getSpaceConfig() {
        return spaceConfig;
    }

    public WorkspaceConfigurationOutgoing setSpaceConfig(KeyValueItem spaceConfig) {
        this.spaceConfig = spaceConfig;
        return this;
    }

    public KeyValueItem getWorkspace() {
        return workspace;
    }

    public WorkspaceConfigurationOutgoing setWorkspace(KeyValueItem workspace) {
        this.workspace = workspace;
        return this;
    }
}
