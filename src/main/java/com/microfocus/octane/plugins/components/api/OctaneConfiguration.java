package com.microfocus.octane.plugins.components.api;

public class OctaneConfiguration {

	private String baseUrl;
	private String userName;
	private String password;
	private long sharedspaceId;
	private long workspaceId;

	public String getBaseUrl() {
		return baseUrl;
	}

	public OctaneConfiguration setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
		return this;
	}

	public String getUserName() {
		return userName;
	}

	public OctaneConfiguration setUserName(String userName) {
		this.userName = userName;
		return this;
	}

	public String getPassword() {
		return password;
	}

	public OctaneConfiguration setPassword(String password) {
		this.password = password;
		return this;
	}

	public long getSharedspaceId() {
		return sharedspaceId;
	}

	public OctaneConfiguration setSharedspaceId(long sharedspaceId) {
		this.sharedspaceId = sharedspaceId;
		return this;
	}

	public long getWorkspaceId() {
		return workspaceId;
	}

	public OctaneConfiguration setWorkspaceId(long workspaceId) {
		this.workspaceId = workspaceId;
		return this;
	}
}
