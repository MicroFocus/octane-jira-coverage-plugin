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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;


public class OctaneConfigurationManager {

    private static final Logger log = LoggerFactory.getLogger(OctaneConfigurationManager.class);
    private PluginSettingsFactory pluginSettingsFactory;

    private static final String PLUGIN_PREFIX = "com.microfocus.octane.plugins.";
    private static final String CONFIGURATION_KEY = PLUGIN_PREFIX + "configuration";

    private ConfigurationCollection configuration;
    private boolean validConfiguration = true;

    //public static final String DEFAULT_OCTANE_FIELD_UDF = "jira_key_udf";
    public static final String PASSWORD_REPLACE = "__secret__password__"; // NON-NLS

    private static final String PARAM_SHARED_SPACE = "p"; // NON-NLS

    private List<OctaneConfigurationChangedListener> listeners = new ArrayList<OctaneConfigurationChangedListener>();

    private static OctaneConfigurationManager instance = new OctaneConfigurationManager();

    private OctaneConfigurationManager() {

    }

    public void init(PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    public static OctaneConfigurationManager getInstance() {
        return instance;
    }

    public void addListener(OctaneConfigurationChangedListener toAdd) {
        listeners.add(toAdd);
    }

    public SpaceConfiguration getConfiguration() {
        if (configuration == null) {
            log.trace("configuration is null");
            try {
                configuration = loadConfiguration();
                validConfiguration = configuration.getSpaces().get(0).getLocation() != null;
            } catch (Exception e) {
                log.error("failed to getConfiguration : " + e.getClass().getName() + ", message =" + e.getMessage() + ", cause=" + e.getCause());
                validConfiguration = false;
            }
        }
        return configuration.getSpaces().get(0);
    }

    public ProxyConfiguration getProxySettings() {
        return configuration.getProxy();
    }

    public boolean isValidConfiguration() {
        return validConfiguration;
    }

    private ConfigurationCollection loadConfiguration() throws IOException {
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        String confStr = ((String) settings.get(CONFIGURATION_KEY));

        if (confStr == null) {//create initial configuration
            SpaceConfiguration spaceConfiguration = new SpaceConfiguration();
            spaceConfiguration.setId(UUID.randomUUID().toString());
            spaceConfiguration.setWorkspaces(new ArrayList<>());
            configuration = new ConfigurationCollection();
            configuration.setSpaces(Arrays.asList(spaceConfiguration));

            try {
                //check if exist configuration from version 1.2, there we saved each field value in different property
                String OCTANE_LOCATION_KEY = PLUGIN_PREFIX + "octaneUrl";
                String CLIENT_ID_KEY = PLUGIN_PREFIX + "clientId";
                String CLIENT_SECRET_KEY = PLUGIN_PREFIX + "clientSecret";
                //String OCTANE_UDF_FIELD_KEY = PLUGIN_PREFIX + "octaneUdf";
                //String JIRA_ISSUE_TYPES_KEY = PLUGIN_PREFIX + "jiraIssueTypes";
                //String JIRA_PROJECTS_KEY = PLUGIN_PREFIX + "jiraProjects";
                String location = (String) settings.get(OCTANE_LOCATION_KEY);
                if (StringUtils.isNotEmpty(location)) {
                    spaceConfiguration.setLocationParts(parseUiLocation(location));
                    spaceConfiguration.setLocation(location);
                    spaceConfiguration.setClientId((String) settings.get(CLIENT_ID_KEY));
                    spaceConfiguration.setClientSecret((String) settings.get(CLIENT_SECRET_KEY));
                }
            } catch (Exception e) {
                //do nothing
            }

            persistConfiguration();
        } else {
            configuration = JsonHelper.deserialize(confStr, ConfigurationCollection.class);
            configuration.getSpaces().forEach(sc -> {
                sc.getWorkspaces().forEach(wc -> wc.setSpaceConfiguration(sc));
            });
        }

        return configuration;
    }

    private void persistConfiguration() {

        try {
            String confStr = JsonHelper.serialize(configuration);
            int HARD_LIMIT_SIZE = 99000;
            if (confStr.length() >= HARD_LIMIT_SIZE) {
                throw new RuntimeException("Configuration file exceeds hard limit size of " + HARD_LIMIT_SIZE + " characters");
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
        if (!PASSWORD_REPLACE.equals(spaceConfigurationOutgoing.getClientSecret())) {
            spConfig.setClientSecret(spaceConfigurationOutgoing.getClientSecret());
        }

        spConfig.setLocationParts(parseUiLocation(spaceConfigurationOutgoing.getLocation()));
        spConfig.setLocation(spaceConfigurationOutgoing.getLocation());

        persistConfiguration();

        //update listeners
        for (OctaneConfigurationChangedListener hl : listeners) {
            try {
                hl.onOctaneConfigurationChanged();
            } catch (Exception e) {
                log.error(String.format("Failed on onOctaneConfigurationChanged for listener %s: %s", hl.getClass().getSimpleName(), e.getMessage()));
            }
        }
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
        if (!proxyOutgoing.getPassword().equals(PASSWORD_REPLACE)) {
            proxy.setPassword(proxyOutgoing.getPassword());
        }

        configuration.setProxy(proxy);
        persistConfiguration();
    }

    public WorkspaceConfiguration saveWorkspaceConfiguration(WorkspaceConfigurationOutgoing model) {
        SpaceConfiguration spConfig = getConfiguration();
        Optional<WorkspaceConfiguration> opt = spConfig.getWorkspaces().stream().filter(w -> w.getWorkspaceId() == model.getId()).findFirst();
        if (opt.isPresent()) {
            spConfig.getWorkspaces().remove(opt.get());
        }

        WorkspaceConfiguration newWorkspaceConfiguration = new WorkspaceConfiguration();
        newWorkspaceConfiguration.setWorkspaceId(model.getId());
        newWorkspaceConfiguration.setWorkspaceName(model.getWorkspaceName());
        newWorkspaceConfiguration.setOctaneUdf(model.getOctaneUdf());
        newWorkspaceConfiguration.setOctaneEntityTypes(model.getOctaneEntityTypes().stream().map(label -> OctaneEntityTypeManager.getByLabel(label).getTypeName()).collect(Collectors.toList()));
        newWorkspaceConfiguration.setJiraIssueTypes(model.getJiraIssueTypes().stream().sorted().collect(Collectors.toList()));
        newWorkspaceConfiguration.setJiraProjects(model.getJiraProjects().stream().sorted().collect(Collectors.toList()));
        newWorkspaceConfiguration.setSpaceConfiguration(spConfig);

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

    public static LocationParts parseUiLocation(String uiLocation) {
        String errorMsg = null;
        try {
            URL url = new URL(uiLocation);
            int contextPos = uiLocation.toLowerCase().indexOf("/ui");
            if (contextPos < 0) {
                errorMsg = "Location url is missing '/ui' part ";
            } else {
                LocationParts parts = new LocationParts();
                parts.setBaseUrl(uiLocation.substring(0, contextPos));
                Map<String, List<String>> queries = splitQuery(url);

                if (queries.containsKey(PARAM_SHARED_SPACE)) {
                    List<String> sharedSpaceParamValue = queries.get(PARAM_SHARED_SPACE);
                    if (sharedSpaceParamValue != null && !sharedSpaceParamValue.isEmpty()) {
                        String[] sharedSpaceAndWorkspace = sharedSpaceParamValue.get(0).split("/");
                        if (sharedSpaceAndWorkspace.length == 2 /*p=1001/1002*/ || sharedSpaceAndWorkspace.length == 1 /*p=1001*/) {
                            try {
                                long spaceId = Long.parseLong(sharedSpaceAndWorkspace[0].trim());
                                parts.setSpaceId(spaceId);
                                return parts;
                            } catch (NumberFormatException e) {
                                errorMsg = "Space id must be numeric value";
                            }
                        } else {
                            errorMsg = "Location url has invalid sharedspace/workspace part";
                        }
                    }
                } else {
                    errorMsg = "Location url is missing sharedspace id";
                }
            }
        } catch (Exception e) {
            errorMsg = "Location contains invalid URL ";
        }

        throw new IllegalArgumentException(errorMsg);

    }

    private static Map<String, List<String>> splitQuery(URL url) throws UnsupportedEncodingException {
        final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
        final String[] pairs = url.getQuery().split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
            if (!query_pairs.containsKey(key)) {
                query_pairs.put(key, new LinkedList<String>());
            }
            final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
            query_pairs.get(key).add(value);
        }
        return query_pairs;
    }
}
