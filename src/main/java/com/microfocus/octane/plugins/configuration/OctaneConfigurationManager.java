package com.microfocus.octane.plugins.configuration;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class OctaneConfigurationManager {

	private static final String PLUGIN_PREFIX = "com.microfocus.octane.plugins.";
	private static final String OCTANE_LOCATION_KEY = PLUGIN_PREFIX + "octaneUrl";
	private static final String CLIENT_ID_KEY = PLUGIN_PREFIX + "clientId";
	private static final String CLIENT_SECRET_KEY = PLUGIN_PREFIX + "clientSecret";

	private static final String PARAM_SHARED_SPACE = "p"; // NON-NLS

	public static OctaneConfiguration loadDetailsFromGlobalSettings(PluginSettingsFactory pluginSettingsFactory) {
		PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
		OctaneConfiguration config = new OctaneConfiguration();
		config.setLocation((String) settings.get(OCTANE_LOCATION_KEY));
		config.setClientId((String) settings.get(CLIENT_ID_KEY));
		config.setClientSecret((String) settings.get(CLIENT_SECRET_KEY));
		return config;
	}


	public static void saveConfigurationInGlobalSettings(PluginSettingsFactory pluginSettingsFactory, OctaneConfiguration config) {
		PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
		pluginSettings.put(OCTANE_LOCATION_KEY, config.getLocation());
		pluginSettings.put(CLIENT_ID_KEY, config.getClientId());
		pluginSettings.put(CLIENT_SECRET_KEY, config.getClientSecret());
	}

	public static OctaneConfiguration.OctaneDetails parseUiLocation(String uiLocation) {
		OctaneConfiguration.OctaneDetails details = new OctaneConfiguration.OctaneDetails();
		String errorMsg = null;
		try {
			URL url = new URL(uiLocation);
			int contextPos = uiLocation.toLowerCase().indexOf("/ui");
			if (contextPos < 0) {
				errorMsg = "Location url is missing '/ui' part ";
			} else {

				details.setBaseUrl(uiLocation.substring(0, contextPos));
				Map<String, List<String>> queries = splitQuery(url);

				if (queries.containsKey(PARAM_SHARED_SPACE)) {
					List<String> sharedSpaceParamValue = queries.get(PARAM_SHARED_SPACE);
					if (sharedSpaceParamValue != null && !sharedSpaceParamValue.isEmpty()) {
						String[] sharedSpaceAndWorkspace = sharedSpaceParamValue.get(0).split("/");
						if (sharedSpaceAndWorkspace.length == 2) {
							details.setSharedspaceId(sharedSpaceAndWorkspace[0]);
							details.setWorkspaceId(sharedSpaceAndWorkspace[1]);
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
		return details;
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
