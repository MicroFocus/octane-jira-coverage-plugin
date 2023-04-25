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

import com.microfocus.octane.plugins.rest.ProxyConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationCollectionV2 {

    private List<SpaceConfigurationV2> spaces = new ArrayList<>();
    private List<WorkspaceConfigurationV2> workspaces = new ArrayList<>();
    private ProxyConfiguration proxy;

    public List<SpaceConfigurationV2> getSpaces() {
        return spaces;
    }

    public List<WorkspaceConfigurationV2> getWorkspaces() {
        return workspaces;
    }

    public void setSpaces(List<SpaceConfigurationV2> spaces) {
        if (spaces == null) {
            spaces = new ArrayList<>();
        }
        this.spaces = spaces;
    }

    public void setWorkspaces(List<WorkspaceConfigurationV2> workspaces) {
        if (workspaces == null) {
            workspaces = new ArrayList<>();
        }
        this.workspaces = workspaces;
    }

    public ProxyConfiguration getProxy() {
        return proxy;
    }

    public void setProxy(ProxyConfiguration proxy) {
        this.proxy = proxy;
    }
}
