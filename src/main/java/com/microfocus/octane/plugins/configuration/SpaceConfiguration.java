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


public class SpaceConfiguration {

    private String location;
    private LocationParts locationParts;
    private String clientId;
    private String clientSecret;

    private String id;

    private List<WorkspaceConfiguration> workspaces;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<WorkspaceConfiguration> getWorkspaces() {
        return workspaces;
    }

    public void setWorkspaces(List<WorkspaceConfiguration> workspaces) {
        this.workspaces = workspaces;
    }

    public LocationParts getLocationParts() {
        return locationParts;
    }

    public void setLocationParts(LocationParts locationParts) {
        this.locationParts = locationParts;
    }
}
