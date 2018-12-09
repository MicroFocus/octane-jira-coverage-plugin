package com.microfocus.octane.plugins.components.api;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.microfocus.octane.plugins.rest.OctaneHttpHelper;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonProperty;


public class OctaneConfiguration {

	private static final String PLUGIN_PREFIX = "com.microfocus.octane.plugins.";
	private static final String OCTANE_LOCATION_KEY = PLUGIN_PREFIX + "octaneUrl";
	private static final String CLIENT_ID_KEY = PLUGIN_PREFIX + "clientId";
	private static final String CLIENT_SECRET_KEY = PLUGIN_PREFIX + "clientSecret";

	private String location;
	private String clientId;
	private String clientSecret;

	private OctaneHttpHelper.OctaneDetails details;

	@JsonProperty("location")
	public String getLocation() {
		return location;
	}

	@JsonProperty("location")
	public void setLocation(String location) {
		this.location = location;
	}

	@JsonProperty("clientSecret")
	public String getClientSecret() {
		return clientSecret;
	}

	@JsonProperty("clientSecret")
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	@JsonProperty("clientId")
	public String getClientId() {
		return clientId;
	}

	@JsonProperty("clientId")
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getBaseUrl() {
		if (details != null) {
			return details.getBaseUrl();
		}
		return null;
	}

	public String getSharespaceId() {
		if (details != null) {
			return details.getSharedspaceId();
		}
		return null;
	}

	public String getWorkspaceId() {
		if (details != null) {
			return details.getSharedspaceId();
		}
		return null;
	}

	public static OctaneConfiguration loadDetailsFromGlobalSettings(PluginSettingsFactory pluginSettingsFactory) {
		PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
		OctaneConfiguration config = new OctaneConfiguration();
		config.setLocation((String) settings.get(OCTANE_LOCATION_KEY));
		config.setClientId((String) settings.get(CLIENT_ID_KEY));
		config.setClientSecret((String) settings.get(CLIENT_SECRET_KEY));
		return config;
	}

	public boolean parseLocation() {
		try {
			this.details = OctaneHttpHelper.parseUiLocation(this.getLocation());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static void saveConfigurationInGlobalSettings(PluginSettingsFactory pluginSettingsFactory, OctaneConfiguration config) {
		PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
		pluginSettings.put(OCTANE_LOCATION_KEY, config.getLocation());
		pluginSettings.put(CLIENT_ID_KEY, config.getClientId());
		pluginSettings.put(CLIENT_SECRET_KEY, config.getClientSecret());
	}


}
