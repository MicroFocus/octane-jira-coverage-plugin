package com.microfocus.octane.plugins.configuration;

import org.codehaus.jackson.annotate.JsonProperty;


public class OctaneConfiguration implements Cloneable {

	private String location;
	private String clientId;
	private String clientSecret;
	private String octaneUdf = "jira_key_udf";

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

	@JsonProperty("octaneUdf")
	public String getOctaneUdf() {
		return octaneUdf;
	}

	@JsonProperty("octaneUdf")
	public void setOctaneUdf(String octaneUdf) {
		this.octaneUdf = octaneUdf;
	}

	public String getBaseUrl() {
		if (details != null) {
			return details.getBaseUrl();
		} else if (parseLocation()) {
			return details.getBaseUrl();
		}
		return null;
	}

	public String getSharespaceId() {
		if (details != null) {
			return details.getSharedspaceId();
		} else if (parseLocation()) {
			return details.getSharedspaceId();

		}
		return null;
	}

	public String getWorkspaceId() {
		if (details != null) {
			return details.getWorkspaceId();
		} else if (parseLocation()) {
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

	public OctaneConfiguration clone() {
		try {
			return (OctaneConfiguration) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;//not possible
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
