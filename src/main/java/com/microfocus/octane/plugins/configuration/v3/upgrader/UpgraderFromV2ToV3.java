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
package com.microfocus.octane.plugins.configuration.v3.upgrader;

import com.microfocus.octane.plugins.configuration.v3.*;
import com.microfocus.octane.plugins.configuration.OctaneWorkspace;
import com.microfocus.octane.plugins.configuration.v2.ConfigurationCollectionV2;
import com.microfocus.octane.plugins.tools.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class UpgraderFromV2ToV3 {

    private static final Logger log = LoggerFactory.getLogger(UpgraderFromV2ToV3.class);

    public static ConfigurationCollection upgradeConfigurationFromV2ToV3(String confStrV2) {
        ConfigurationCollection tempConfiguration = null;

        try {
            ConfigurationCollectionV2 configurationV2 = JsonHelper.deserialize(confStrV2, ConfigurationCollectionV2.class);

            if (!configurationV2.getSpaces().isEmpty()) {

                List<SpaceConfiguration> spaceConfigurations = configurationV2.getSpaces().stream()
                        .map(spaceConfigurationV2 -> new SpaceConfiguration(
                                spaceConfigurationV2.getName(),
                                spaceConfigurationV2.getLocation(),
                                spaceConfigurationV2.getLocationParts(),
                                spaceConfigurationV2.getClientId(),
                                spaceConfigurationV2.getClientSecret(),
                                spaceConfigurationV2.getId()))
                        .collect(Collectors.toList());

                List<WorkspaceConfiguration> workspaceConfigurations = configurationV2.getWorkspaces().stream()
                        .map(workspaceConfigurationV2 -> new WorkspaceConfiguration(
                                workspaceConfigurationV2.getId(),
                                workspaceConfigurationV2.getSpaceConfigurationId(),
                                new OctaneConfigGrouping(
                                        new HashSet<>(List.of(
                                                new OctaneWorkspace(
                                                        String.valueOf(workspaceConfigurationV2.getWorkspaceId()),
                                                        workspaceConfigurationV2.getWorkspaceName()
                                                ))),
                                        workspaceConfigurationV2.getOctaneUdf(),
                                        workspaceConfigurationV2.getOctaneEntityTypes()),
                                new JiraConfigGrouping(
                                        workspaceConfigurationV2.getJiraProjects(),
                                        workspaceConfigurationV2.getJiraIssueTypes()
                                )))
                        .collect(Collectors.toList());

                tempConfiguration = new ConfigurationCollection();
                tempConfiguration.setSpaces(spaceConfigurations);
                tempConfiguration.setWorkspaces(workspaceConfigurations);

                if (configurationV2.getProxy() != null) {
                    tempConfiguration.setProxy(configurationV2.getProxy());
                }
            }
        } catch (Exception e) {
            log.error("Failed tryConvertFromConfigurationV2: " + e.getMessage());
        }

        if (tempConfiguration == null) {
            tempConfiguration = new ConfigurationCollection();
        }

        return tempConfiguration;
    }
}
