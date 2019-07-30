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

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.microfocus.octane.plugins.admin.ProxyConfigurationOutgoing;
import com.microfocus.octane.plugins.admin.SpaceConfigurationOutgoing;
import com.microfocus.octane.plugins.admin.WorkspaceConfigurationOutgoing;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeManager;
import com.microfocus.octane.plugins.rest.ProxyConfiguration;
import com.microfocus.octane.plugins.tools.JsonHelper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class OctaneConfigurationManager {

    public static final String SHOW_DEBUG_PARAMETER = "showDebug";
    private static final Logger log = LoggerFactory.getLogger(OctaneConfigurationManager.class);
    private PluginSettingsFactory pluginSettingsFactory;

    private static final String PLUGIN_PREFIX = "com.microfocus.octane.plugins.";
    private static final String CONFIGURATION_KEY = PLUGIN_PREFIX + "configuration";
    private static final String USER_FILTER_KEY = PLUGIN_PREFIX + "user.filter";

    private ConfigurationCollection configuration;

    private static int CONFIGURATION_HARD_LIMIT_SIZE = 99000;

    //public static final String DEFAULT_OCTANE_FIELD_UDF = "jira_key_udf";


    private static Map<String, String> username2Filter;
    private static Map<String, Map<String, Object>> username2parameters = new HashMap<>();

    private static OctaneConfigurationManager instance = new OctaneConfigurationManager();

    private OctaneConfigurationManager() {

    }

    public void init(PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        loadConfiguration();
    }

    public static OctaneConfigurationManager getInstance() {
        return instance;
    }


    public Optional<SpaceConfiguration> getSpaceConfigurationById(String spaceConfigurationId) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(spaceConfigurationId)) {
            throw new IllegalArgumentException("Space configuration id should not be empty");
        }
        return configuration.getSpaces().stream().filter(s -> s.getId().equals(spaceConfigurationId)).findFirst();
    }

    public List<SpaceConfiguration> getSpaceConfigurations() {
        return configuration.getSpaces();
    }

    @Deprecated
    public SpaceConfiguration getConfiguration() {
        return configuration.getSpaces().get(0);
    }

    public ProxyConfiguration getProxySettings() {
        return configuration.getProxy();
    }

    private ConfigurationCollection loadConfiguration() {
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        String confStr = ((String) settings.get(CONFIGURATION_KEY));

        if (confStr == null) {//create initial configuration
            configuration = new ConfigurationCollection();
            persistConfiguration();
        } else {
            try {
                configuration = JsonHelper.deserialize(confStr, ConfigurationCollection.class);
            } catch (IOException e) {
                configuration = new ConfigurationCollection();
                log.error("Failed to deserialize configuration in loadConfiguration : " + e.getMessage());
            }
        }

        return configuration;
    }

    private void persistConfiguration() {

        try {
            String confStr = JsonHelper.serialize(configuration);
            if (confStr.length() >= CONFIGURATION_HARD_LIMIT_SIZE) {
                throw new RuntimeException("Configuration file exceeds hard limit size of " + CONFIGURATION_HARD_LIMIT_SIZE + " characters");
            }
            PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
            settings.put(CONFIGURATION_KEY, confStr);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist configuration :" + e.getMessage());
        }
    }

    public void saveSpaceConfiguration(SpaceConfigurationOutgoing spaceConfigurationOutgoing) {
        SpaceConfiguration spConfig = getConfiguration();
        spConfig.setClientId(spaceConfigurationOutgoing.getClientId());
        if (!PluginConstants.PASSWORD_REPLACE.equals(spaceConfigurationOutgoing.getClientSecret())) {
            spConfig.setClientSecret(spaceConfigurationOutgoing.getClientSecret());
        }

        spConfig.setLocationParts(ConfigurarionUtil.parseUiLocation(spaceConfigurationOutgoing.getLocation()));
        spConfig.setLocation(spaceConfigurationOutgoing.getLocation());

        persistConfiguration();
    }

    public void saveProxyConfiguration(ProxyConfigurationOutgoing proxyOutgoing) {
        ProxyConfiguration proxy = getProxySettings();
        if (proxy == null) {
            proxy = new ProxyConfiguration();
        }

        proxy.setHost(proxyOutgoing.getHost());
        Integer port = null;
        if (StringUtils.isNotEmpty(proxyOutgoing.getHost()) && StringUtils.isNotEmpty(proxyOutgoing.getPort())) {
            try {
                port = Integer.parseInt(proxyOutgoing.getPort());
            } catch (NumberFormatException e) {
                //do nothing
            }
        }
        proxy.setPort(port);
        proxy.setUsername(proxyOutgoing.getUsername());
        if (!proxyOutgoing.getPassword().equals(PluginConstants.PASSWORD_REPLACE)) {
            proxy.setPassword(proxyOutgoing.getPassword());
        }

        configuration.setProxy(proxy);
        persistConfiguration();
    }

    public WorkspaceConfiguration saveWorkspaceConfiguration(WorkspaceConfigurationOutgoing model) {
        SpaceConfiguration spConfig = getConfiguration();
        //Optional<WorkspaceConfiguration> opt = spConfig.getWorkspaces().stream().filter(w -> w.getWorkspaceId() == model.getId()).findFirst();
        //if (opt.isPresent()) {
        //    spConfig.getWorkspaces().remove(opt.get());
        //}

        WorkspaceConfiguration newWorkspaceConfiguration = new WorkspaceConfiguration();
        newWorkspaceConfiguration.setWorkspaceId(Long.parseLong(model.getWorkspace().getId()));
        newWorkspaceConfiguration.setWorkspaceName(model.getWorkspace().getText());
        newWorkspaceConfiguration.setOctaneUdf(model.getOctaneUdf());
        newWorkspaceConfiguration.setOctaneEntityTypes(model.getOctaneEntityTypes().stream().map(label -> OctaneEntityTypeManager.getByLabel(label).getTypeName()).collect(Collectors.toList()));
        newWorkspaceConfiguration.setJiraIssueTypes(model.getJiraIssueTypes().stream().sorted().collect(Collectors.toList()));
        newWorkspaceConfiguration.setJiraProjects(model.getJiraProjects().stream().sorted().collect(Collectors.toList()));
        //newWorkspaceConfiguration.setSpaceConfigurationId(spConfig);

        spConfig.getWorkspaces().add(newWorkspaceConfiguration);
        persistConfiguration();
        return newWorkspaceConfiguration;
    }

    public boolean deleteWorkspaceConfiguration(long id) {
        SpaceConfiguration spConfig = getConfiguration();
        Optional<WorkspaceConfiguration> opt = spConfig.getWorkspaces().stream().filter(w -> w.getWorkspaceId() == id).findFirst();
        if (opt.isPresent()) {
            spConfig.getWorkspaces().remove(opt.get());
            persistConfiguration();
            return true;
        }
        return false;
    }

    public void setUserFilter(String username, String filter) {
        String existing = getUserFilter(username);
        if (!StringUtils.equals(existing, filter)) {
            if (StringUtils.isEmpty(filter)) {
                username2Filter.remove(username);
            } else {
                username2Filter.put(username, filter);
            }

            //persist
            try {
                String confStr = JsonHelper.serialize(username2Filter);
                if (confStr.length() >= CONFIGURATION_HARD_LIMIT_SIZE) {
                    throw new RuntimeException("User filter configuration file exceeds hard limit size of " + CONFIGURATION_HARD_LIMIT_SIZE + " characters");
                }
                PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
                settings.put(USER_FILTER_KEY, confStr);
            } catch (IOException e) {
                throw new RuntimeException("Failed to persist user filter configuration :" + e.getMessage());
            }
        }
    }

    public String getUserFilter(String username) {
        if (username2Filter == null) {
            PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
            String str = ((String) settings.get(USER_FILTER_KEY));
            if (str == null) {
                username2Filter = new HashMap<>();
            } else {
                try {
                    username2Filter = JsonHelper.deserialize(str, Map.class);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to deserialize user filter configuration :" + e.getMessage());
                }
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


}
