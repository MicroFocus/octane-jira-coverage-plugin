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
