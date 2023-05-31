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
package com.microfocus.octane.plugins.configuration.v2.upgrader;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.microfocus.octane.plugins.configuration.ConfigurationUtil;
import com.microfocus.octane.plugins.configuration.LocationParts;
import com.microfocus.octane.plugins.configuration.v1.ConfigurationCollectionV1;
import com.microfocus.octane.plugins.configuration.v1.SpaceConfigurationV1;
import com.microfocus.octane.plugins.configuration.v2.ConfigurationCollectionV2;
import com.microfocus.octane.plugins.configuration.v2.SpaceConfigurationV2;
import com.microfocus.octane.plugins.configuration.v2.WorkspaceConfigurationV2;
import com.microfocus.octane.plugins.tools.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.microfocus.octane.plugins.configuration.ConfigurationManagerConstants.*;

public class UpgraderFromV1ToV2 {

    private static final Logger log = LoggerFactory.getLogger(UpgraderFromV1ToV2.class);

    public static void upgradeConfigurationFromV1ToV2(PluginSettings settings, PluginSettingsFactory pluginSettingsFactory) {
        ConfigurationCollectionV2 tempConfiguration = null;
        String confStrV1 = ((String) settings.get(CONFIGURATION_KEY_V1));

        if (confStrV1 != null) {
            try {
                ConfigurationCollectionV1 configurationV1 = JsonHelper.deserialize(confStrV1, ConfigurationCollectionV1.class);

                if (configurationV1.getSpaces().size() == 1) {
                    SpaceConfigurationV1 scV1 = configurationV1.getSpaces().get(0);
                    LocationParts locationParts = ConfigurationUtil.parseUiLocation(scV1.getLocation());

                    SpaceConfigurationV2 sc = new SpaceConfigurationV2()
                            .setId(UUID.randomUUID().toString())
                            .setClientId(scV1.getClientId())
                            .setClientSecret(scV1.getClientSecret())
                            .setLocation(scV1.getLocation())
                            .setLocationParts(locationParts)
                            .setName(locationParts.getKey());

                    List<WorkspaceConfigurationV2> wcList = new ArrayList<>();
                    configurationV1.getSpaces().get(0).getWorkspaces().forEach(w -> {
                        WorkspaceConfigurationV2 wc = new WorkspaceConfigurationV2()
                                .setId(UUID.randomUUID().toString())
                                .setJiraIssueTypes(w.getJiraIssueTypes())
                                .setJiraProjects(w.getJiraProjects())
                                .setOctaneEntityTypes(w.getOctaneEntityTypes())
                                .setOctaneUdf(w.getOctaneUdf())
                                .setWorkspaceId(w.getWorkspaceId())
                                .setWorkspaceName(w.getWorkspaceName())
                                .setSpaceConfigurationId(sc.getId());
                        wcList.add(wc);
                    });

                    tempConfiguration = new ConfigurationCollectionV2();
                    tempConfiguration.setSpaces(new ArrayList<>(Arrays.asList(sc)));
                    tempConfiguration.setWorkspaces(wcList);

                    if (configurationV1.getProxy() != null) {
                        tempConfiguration.setProxy(configurationV1.getProxy());
                        tempConfiguration.getProxy().setNonProxyHost("");
                    }
                }
            } catch (Exception e) {
                log.error("Failed tryConvertFromConfigurationV1ToV2: " + e.getMessage());
            }
        }

        persistConfigurationV2(tempConfiguration, pluginSettingsFactory);
    }

    private static void persistConfigurationV2(ConfigurationCollectionV2 configuration, PluginSettingsFactory pluginSettingsFactory) {
        String confStr = JsonHelper.serialize(configuration);

        if (confStr.length() >= CONFIGURATION_HARD_LIMIT_SIZE) {
            throw new RuntimeException("Configuration file exceeds hard limit size of " + CONFIGURATION_HARD_LIMIT_SIZE + " characters");
        }

        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        settings.put(CONFIGURATION_KEY_V2, confStr);
    }
}
