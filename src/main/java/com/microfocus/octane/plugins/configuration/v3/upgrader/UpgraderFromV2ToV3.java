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
