package com.microfocus.octane.plugins.configuration;

import org.codehaus.jackson.annotate.JsonProperty;


public class OctaneConfiguration {

	private String location;
	private String clientId;
	private String clientSecret;

	private OctaneDetails details;

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
			return details.getWorkspaceId();
		}
		return null;
	}

	public boolean parseLocation() {
		try {
			this.details = OctaneConfigurationManager.parseUiLocation(this.getLocation());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static final class OctaneDetails {
		private String baseUrl;
		private String sharedspaceId;
		private String workspaceId;

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public String getSharedspaceId() {
			return sharedspaceId;
		}

		public void setSharedspaceId(String sharedspaceId) {
			this.sharedspaceId = sharedspaceId;
		}

		public String getWorkspaceId() {
			return workspaceId;
		}

		public void setWorkspaceId(String workspaceId) {
			this.workspaceId = workspaceId;
		}
	}
}
