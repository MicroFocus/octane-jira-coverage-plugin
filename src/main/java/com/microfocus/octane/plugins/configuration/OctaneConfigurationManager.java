package com.microfocus.octane.plugins.configuration;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class OctaneConfigurationManager {

    private static final Logger log = LoggerFactory.getLogger(OctaneConfigurationManager.class);
    private PluginSettingsFactory pluginSettingsFactory;

    private static final String PLUGIN_PREFIX = "com.microfocus.octane.plugins.";
    private static final String OCTANE_LOCATION_KEY = PLUGIN_PREFIX + "octaneUrl";
    private static final String CLIENT_ID_KEY = PLUGIN_PREFIX + "clientId";
    private static final String CLIENT_SECRET_KEY = PLUGIN_PREFIX + "clientSecret";
    private static final String OCTANE_UDF_FIELD_KEY = PLUGIN_PREFIX + "octaneUdf";
    private static final String JIRA_ISSUE_TYPES_KEY = PLUGIN_PREFIX + "jiraIssueTypes";
    private static final String JIRA_PROJECTS_KEY = PLUGIN_PREFIX + "jiraProjects";


    public static final String DEFAULT_OCTANE_FIELD_UDF = "jira_key_udf";
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

    private static OctaneConfiguration externalConfig;

    private static boolean validConfiguration = true;

    public OctaneConfiguration getConfiguration() {
        if (externalConfig == null) {
            try {
                externalConfig = convertToInternalConfiguration(loadConfiguration());
                validConfiguration = true;
            } catch (Exception e) {
                validConfiguration = false;
            }
        }
        return externalConfig;
    }

    public boolean isValidConfiguration() {
        return validConfiguration;
    }

    public OctaneConfigurationOutgoing loadConfiguration() {
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        OctaneConfigurationOutgoing outgoingConfiguration = new OctaneConfigurationOutgoing();
        outgoingConfiguration.setLocation((String) settings.get(OCTANE_LOCATION_KEY));
        outgoingConfiguration.setClientId((String) settings.get(CLIENT_ID_KEY));
        outgoingConfiguration.setClientSecret((String) settings.get(CLIENT_SECRET_KEY));
        outgoingConfiguration.setOctaneUdf((String) settings.get(OCTANE_UDF_FIELD_KEY));
        outgoingConfiguration.setJiraIssueTypes((String) settings.get(JIRA_ISSUE_TYPES_KEY));
        outgoingConfiguration.setJiraProjects((String) settings.get(JIRA_PROJECTS_KEY));
        return outgoingConfiguration;
    }

    public void saveConfiguration(OctaneConfigurationOutgoing config) {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        pluginSettings.put(OCTANE_LOCATION_KEY, config.getLocation());
        pluginSettings.put(CLIENT_ID_KEY, config.getClientId());
        pluginSettings.put(OCTANE_UDF_FIELD_KEY, config.getOctaneUdf());
        pluginSettings.put(JIRA_ISSUE_TYPES_KEY, config.getJiraIssueTypes());
        pluginSettings.put(JIRA_PROJECTS_KEY, config.getJiraProjects());

        if (!PASSWORD_REPLACE.equals(config.getClientSecret())) {
            pluginSettings.put(CLIENT_SECRET_KEY, config.getClientSecret());
        }

        externalConfig = null;
        //update listeners
        for (OctaneConfigurationChangedListener hl : listeners) {
            try {
                hl.onOctaneConfigurationChanged();
            } catch (Exception e) {
                log.error(String.format("Failed on onOctaneConfigurationChanged for listener %s: %s", hl.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }

    public OctaneConfiguration convertToInternalConfiguration(OctaneConfigurationOutgoing outgoing) {
        OctaneConfiguration internal = new OctaneConfiguration();

        internal.setClientId(outgoing.getClientId());
        internal.setOctaneUdf(outgoing.getOctaneUdf());

        //handle url location
        parseUiLocation(internal, outgoing.getLocation());

        //set jira issue type
        String issueTypes = outgoing.getJiraIssueTypes().toLowerCase().trim();
        if (StringUtils.isEmpty(issueTypes)) {
            internal.setJiraIssueTypes(Collections.emptySet());
        } else {
            internal.setJiraIssueTypes(Stream.of(issueTypes.split(",")).map(String::trim).collect(Collectors.toSet()));
        }

        //set jira projects
        String project = outgoing.getJiraProjects().toUpperCase().trim();
        if (StringUtils.isEmpty(project)) {
            internal.setJiraProjects(Collections.emptySet());
        } else {
            internal.setJiraProjects(Stream.of(project.split(",")).map(String::trim).collect(Collectors.toSet()));
        }

        //replace password
        if (PASSWORD_REPLACE.equals(outgoing.getClientSecret())) {
            PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
            internal.setClientSecret((String) settings.get(CLIENT_SECRET_KEY));
        } else {
            internal.setClientSecret(outgoing.getClientSecret());
        }

        return internal;
    }

    private static void parseUiLocation(OctaneConfiguration internal, String uiLocation) {
        String errorMsg = null;
        try {
            URL url = new URL(uiLocation);
            int contextPos = uiLocation.toLowerCase().indexOf("/ui");
            if (contextPos < 0) {
                errorMsg = "Location url is missing '/ui' part ";
            } else {

                internal.setBaseUrl(uiLocation.substring(0, contextPos));
                Map<String, List<String>> queries = splitQuery(url);

                if (queries.containsKey(PARAM_SHARED_SPACE)) {
                    List<String> sharedSpaceParamValue = queries.get(PARAM_SHARED_SPACE);
                    if (sharedSpaceParamValue != null && !sharedSpaceParamValue.isEmpty()) {
                        String[] sharedSpaceAndWorkspace = sharedSpaceParamValue.get(0).split("/");
                        if (sharedSpaceAndWorkspace.length == 2) {
                            internal.setSharedspaceId(sharedSpaceAndWorkspace[0]);
                            internal.setWorkspaceId(sharedSpaceAndWorkspace[1]);
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
        if (errorMsg != null) {
            throw new IllegalArgumentException(errorMsg);
        }
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
