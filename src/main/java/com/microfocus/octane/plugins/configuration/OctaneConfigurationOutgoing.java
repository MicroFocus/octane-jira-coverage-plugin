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

import org.codehaus.jackson.annotate.JsonProperty;


public class OctaneConfigurationOutgoing implements Cloneable {


    private String location;
    private String clientId;
    private String clientSecret;
    private String octaneUdf;
    private String jiraIssueTypes;
    private String jiraProjects;


    @JsonProperty("location")
    public String getLocation() {
        return location;
    }

    @JsonProperty("location")
    public void setLocation(String location) {
        this.location = location;
    }

    @JsonProperty("clientSecret")
    public String getClientSecret() {
        return clientSecret;
    }

    @JsonProperty("clientSecret")
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @JsonProperty("clientId")
    public String getClientId() {
        return clientId;
    }

    @JsonProperty("clientId")
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonProperty("octaneUdf")
    public String getOctaneUdf() {
        return octaneUdf;
    }

    @JsonProperty("octaneUdf")
    public void setOctaneUdf(String octaneUdf) {
        this.octaneUdf = octaneUdf;
    }

    @JsonProperty("jiraIssueTypes")
    public String getJiraIssueTypes() {
        return jiraIssueTypes;
    }

    @JsonProperty("jiraIssueTypes")
    public void setJiraIssueTypes(String jiraIssueTypes) {
        this.jiraIssueTypes = jiraIssueTypes;
    }

    @JsonProperty("jiraProjects")
    public String getJiraProjects() {
        return jiraProjects;
    }

    @JsonProperty("jiraProjects")
    public void setJiraProjects(String jiraProjects) {
        this.jiraProjects = jiraProjects;
    }


}
