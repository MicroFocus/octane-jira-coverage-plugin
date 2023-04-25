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

package com.microfocus.octane.plugins.configuration.v3;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkspaceConfiguration {

    private String id;
    private String spaceConfigurationId;
    private OctaneConfigGrouping octaneConfigGrouping;
    private JiraConfigGrouping jiraConfigGrouping;

    public WorkspaceConfiguration() {
    }

    public WorkspaceConfiguration(String id, String spaceConfigurationId, OctaneConfigGrouping octaneConfigGrouping, JiraConfigGrouping jiraConfigGrouping) {
        this.id = id;
        this.spaceConfigurationId = spaceConfigurationId;
        this.octaneConfigGrouping = octaneConfigGrouping;
        this.jiraConfigGrouping = jiraConfigGrouping;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSpaceConfigurationId() {
        return spaceConfigurationId;
    }

    public void setSpaceConfigurationId(String spaceConfigurationId) {
        this.spaceConfigurationId = spaceConfigurationId;
    }

    public OctaneConfigGrouping getOctaneConfigGrouping() {
        return octaneConfigGrouping;
    }

    public void setOctaneConfigGrouping(OctaneConfigGrouping octaneConfigGrouping) {
        this.octaneConfigGrouping = octaneConfigGrouping;
    }

    public JiraConfigGrouping getJiraConfigGrouping() {
        return jiraConfigGrouping;
    }

    public void setJiraConfigGrouping(JiraConfigGrouping jiraConfigsGrouping) {
        this.jiraConfigGrouping = jiraConfigsGrouping;
    }
}
