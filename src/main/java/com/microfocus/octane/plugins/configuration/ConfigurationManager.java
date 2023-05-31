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

package com.microfocus.octane.plugins.configuration;

import com.atlassian.jira.cluster.ClusterMessageConsumer;
import com.atlassian.jira.cluster.ClusterMessagingService;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.microfocus.octane.plugins.admin.ProxyConfigurationOutgoing;
import com.microfocus.octane.plugins.configuration.v2.upgrader.UpgraderFromV1ToV2;
import com.microfocus.octane.plugins.configuration.v3.ConfigurationCollection;
import com.microfocus.octane.plugins.configuration.v3.SpaceConfiguration;
import com.microfocus.octane.plugins.configuration.v3.WorkspaceConfiguration;
import com.microfocus.octane.plugins.configuration.v3.upgrader.UpgraderFromV2ToV3;
import com.microfocus.octane.plugins.rest.ProxyConfiguration;
import com.microfocus.octane.plugins.tools.JsonHelper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microfocus.octane.plugins.configuration.ConfigurationManagerConstants.*;


public class ConfigurationManager implements ClusterMessageConsumer {

    public static final String SHOW_DEBUG_PARAMETER = "showDebug";
    private static final Logger log = LoggerFactory.getLogger(ConfigurationManager.class);
    private PluginSettingsFactory pluginSettingsFactory;
    private ClusterMessagingService clusterMessagingService;

    private ConfigurationCollection configuration;

    //public static final String DEFAULT_OCTANE_FIELD_UDF = "jira_key_udf";

    private static Map<String, String> username2Filter;
    private static Map<String, Map<String, Object>> username2parameters = new HashMap<>();

    private static ConfigurationManager instance = new ConfigurationManager();

    private ConfigurationManager() {

    }

    public void init(PluginSettingsFactory pluginSettingsFactory, ClusterMessagingService clusterMessagingService) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.clusterMessagingService = clusterMessagingService;

        clusterMessagingService.registerListener(MESSAGE_CHANNEL, this);
    }

    private synchronized ConfigurationCollection getConfiguration() {
        if (configuration == null) {
            loadConfiguration();
        }
        return configuration;
    }

    public static ConfigurationManager getInstance() {
        return instance;
    }

    public Optional<SpaceConfiguration> getSpaceConfigurationById(String spaceConfigurationId, boolean throwIfNotFound) {
        if (StringUtils.isEmpty(spaceConfigurationId)) {
            throw new IllegalArgumentException("Space configuration id should not be empty");
        }

        Optional<SpaceConfiguration> opt = getConfiguration().getSpaces().stream().filter(s -> s.getId().equals(spaceConfigurationId)).findFirst();
        if (throwIfNotFound && !opt.isPresent()) {
            throw new IllegalArgumentException(String.format("Space configuration with id %s - not found", spaceConfigurationId));
        }

        return opt;
    }

    public Optional<WorkspaceConfiguration> getWorkspaceConfigurationById(String workspaceConfigurationId, boolean throwIfNotFound) {
        if (StringUtils.isEmpty(workspaceConfigurationId)) {
            throw new IllegalArgumentException("Workspace configuration id should not be empty");
        }

        Optional<WorkspaceConfiguration> opt = getConfiguration().getWorkspaces()
                .stream()
                .filter(s -> s.getId().equals(workspaceConfigurationId))
                .findFirst();

        if (throwIfNotFound && !opt.isPresent()) {
            throw new IllegalArgumentException(String.format("Workspace configuration with id %s - not found", workspaceConfigurationId));
        }

        return opt;
    }

    public List<SpaceConfiguration> getSpaceConfigurations() {
        return getConfiguration().getSpaces();
    }

    public SpaceConfiguration addSpaceConfiguration(SpaceConfiguration spaceConfiguration) {
        getConfiguration().getSpaces().add(spaceConfiguration);
        persistConfiguration();
        return spaceConfiguration;
    }

    public SpaceConfiguration updateSpaceConfiguration(SpaceConfiguration updatedSpaceConfiguration) {
        SpaceConfiguration conf = getSpaceConfigurationById(updatedSpaceConfiguration.getId(), true).get();
        getConfiguration().getSpaces().remove(conf);
        getConfiguration().getSpaces().add(updatedSpaceConfiguration);
        persistConfiguration();
        return updatedSpaceConfiguration;
    }

    public boolean removeSpaceConfiguration(String spaceConfigurationId) {
        Optional<SpaceConfiguration> opt = getSpaceConfigurationById(spaceConfigurationId, false);
        if (opt.isPresent()) {
            List<WorkspaceConfiguration> workspaceConfigs = getConfiguration().getWorkspaces().stream()
                    .filter(wc -> spaceConfigurationId.equals(wc.getSpaceConfigurationId()))
                    .collect(Collectors.toList());
            getConfiguration().getSpaces().remove(opt.get());
            getConfiguration().getWorkspaces().removeAll(workspaceConfigs);
            persistConfiguration();
        }
        return opt.isPresent();
    }

    public ProxyConfiguration getProxySettings() {
        return getConfiguration().getProxy();
    }

    public void saveProxyConfiguration(ProxyConfigurationOutgoing proxyOutgoing) {
        ProxyConfiguration proxy = getProxySettings();
        if (proxy == null) {
            proxy = new ProxyConfiguration();
        }

        String host = proxyOutgoing.getHost();
        Integer port = null;
        if (StringUtils.isNotEmpty(proxyOutgoing.getHost()) && StringUtils.isNotEmpty(proxyOutgoing.getPort())) {
            host = host.trim();

            try {
                port = Integer.parseInt(proxyOutgoing.getPort());
            } catch (NumberFormatException e) {
                //do nothing
            }
        }

        proxy.setHost(host);
        proxy.setPort(port);
        proxy.setUsername(proxyOutgoing.getUsername());

        if (!proxyOutgoing.getPassword().equals(PluginConstants.PASSWORD_REPLACE)) {
            proxy.setPassword(proxyOutgoing.getPassword());
        }

        proxy.setNonProxyHost(proxyOutgoing.getNonProxyHost());
        getConfiguration().setProxy(proxy);
        persistConfiguration();
        getSpaceConfigurations().forEach(SpaceConfiguration::clearRestConnector);
    }

    public synchronized void clearConfiguration() {
        log.info("configuration is cleared");
        configuration = null;
    }

    private ConfigurationCollection loadConfiguration() {
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        String confStr = readConfigurationForDataKey(settings, CONFIGURATION_KEY_V3);
        log.info("Configuration is loading...");

        if (confStr == null) {
            configuration = upgradeConfigurationFromPreviousVersions(settings);

            if (configuration == null) {
                configuration = new ConfigurationCollection();
            }

            log.info("Initial configuration is loaded.");
            persistConfiguration();
        } else {
            try {
                configuration = JsonHelper.deserialize(confStr, ConfigurationCollection.class);
            } catch (Exception e) {
                configuration = new ConfigurationCollection();
                log.error("Failed to deserialize configuration in loadConfiguration : " + e.getMessage());
            }
        }

        return configuration;
    }

    private ConfigurationCollection upgradeConfigurationFromPreviousVersions(PluginSettings settings) {
        String confStrV2 = readConfigurationForDataKey(settings, CONFIGURATION_KEY_V2);

        if (confStrV2 != null) {
            return UpgraderFromV2ToV3.upgradeConfigurationFromV2ToV3(confStrV2);
        } else {
            String confStrV1 = readConfigurationForDataKey(settings, CONFIGURATION_KEY_V1);

            if (confStrV1 != null) {
                UpgraderFromV1ToV2.upgradeConfigurationFromV1ToV2(settings, pluginSettingsFactory);

                String newConfStrV2 = readConfigurationForDataKey(settings, CONFIGURATION_KEY_V2);
                return UpgraderFromV2ToV3.upgradeConfigurationFromV2ToV3(newConfStrV2);
            }
        }

        return null;
    }

    private String readConfigurationForDataKey(PluginSettings settings, String key) {
        return ((String) settings.get(key));
    }

    private void persistConfiguration() {
        String confStr = JsonHelper.serialize(configuration);
        if (confStr.length() >= CONFIGURATION_HARD_LIMIT_SIZE) {
            throw new RuntimeException("Configuration file exceeds hard limit size of " + CONFIGURATION_HARD_LIMIT_SIZE + " characters");
        }

        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        settings.put(CONFIGURATION_KEY_V3, confStr);
        sendConfigurationChangedMessage();
    }

    public List<WorkspaceConfiguration> getWorkspaceConfigurations() {
        return getConfiguration().getWorkspaces();
    }

    public WorkspaceConfiguration addWorkspaceConfiguration(WorkspaceConfiguration wc) {
        getConfiguration().getWorkspaces().add(wc);
        persistConfiguration();
        return wc;
    }

    public WorkspaceConfiguration updateWorkspaceConfiguration(WorkspaceConfiguration updatedWc) {
        WorkspaceConfiguration existingWc = getWorkspaceConfigurationById(updatedWc.getId(), true).get();
        getConfiguration().getWorkspaces().remove(existingWc);
        getConfiguration().getWorkspaces().add(updatedWc);
        persistConfiguration();
        return updatedWc;
    }

    public boolean removeWorkspaceConfiguration(String id) {
        Optional<WorkspaceConfiguration> opt = getWorkspaceConfigurationById(id, false);
        if (opt.isPresent()) {
            getConfiguration().getWorkspaces().remove(opt.get());
            persistConfiguration();
        }
        return opt.isPresent();
    }

    public void setUserFilter(String username, String filter) {
        String existing = getUserFilter(username);
        if (!StringUtils.equals(existing, filter)) {
            if (StringUtils.isEmpty(filter)) {
                username2Filter.remove(username);
            } else {
                username2Filter.put(username, filter);
            }

            String confStr = JsonHelper.serialize(username2Filter);
            if (confStr.length() >= CONFIGURATION_HARD_LIMIT_SIZE) {
                throw new RuntimeException("User filter configuration file exceeds hard limit size of " + CONFIGURATION_HARD_LIMIT_SIZE + " characters");
            }
            PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
            settings.put(USER_FILTER_KEY, confStr);
        }
    }

    public String getUserFilter(String username) {
        if (username2Filter == null) {
            PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
            String str = readConfigurationForDataKey(settings, USER_FILTER_KEY);
            if (str == null) {
                username2Filter = new HashMap<>();
            } else {
                username2Filter = JsonHelper.deserialize(str, Map.class);
            }
        }
        return username2Filter.get(username);
    }

    public void setUserParameter(String username, String parameterName, Object parameterValue) {
        if (!username2parameters.containsKey(username)) {
            username2parameters.put(username, new HashMap<>());
        }

        if (parameterValue == null) {
            username2parameters.get(username).remove(parameterName);
        } else {
            username2parameters.get(username).put(parameterName, parameterValue);
        }

    }

    public Object getUserParameter(String username, String parameterValue, Object defaultValue) {
        Object value = defaultValue;
        Map<String, Object> params = username2parameters.get(username);
        if (params != null && params.containsKey(parameterValue)) {
            value = params.get(parameterValue);
        }
        return value;
    }

    public void sendConfigurationChangedMessage() {
        log.info("sending ConfigurationChangedMessage");
        clusterMessagingService.sendRemote(MESSAGE_CHANNEL, "updated");
    }

    @Override
    public void receive(final String channel, final String message, final String senderId) {
        if (MESSAGE_CHANNEL.equals(channel)) {
            log.info(String.format("message received, channel=%s; message=%s; senderId=%s", channel, message, senderId));
            this.clearConfiguration();
        }
    }

}
