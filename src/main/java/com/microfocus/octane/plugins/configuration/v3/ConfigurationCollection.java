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

import com.microfocus.octane.plugins.rest.ProxyConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationCollection {

    private List<SpaceConfiguration> spaces = new ArrayList<>();
    private List<WorkspaceConfiguration> workspaces = new ArrayList<>();
    private ProxyConfiguration proxy;

    public ConfigurationCollection() {
    }

    public List<SpaceConfiguration> getSpaces() {
        return spaces;
    }

    public List<WorkspaceConfiguration> getWorkspaces() {
        return workspaces;
    }

    public void setSpaces(List<SpaceConfiguration> spaces) {
        if (spaces == null) {
            spaces = new ArrayList<>();
        }
        this.spaces = spaces;
    }

    public void setWorkspaces(List<WorkspaceConfiguration> workspaces) {
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
