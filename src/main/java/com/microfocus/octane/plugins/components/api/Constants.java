package com.microfocus.octane.plugins.components.api;

public interface Constants {

	String URL_AUTHENTICATION = "/authentication/sign_in";

	String PUBLIC_API = "/api";
	String PUBLIC_API_SHAREDSPACE_FORMAT = PUBLIC_API + "/shared_spaces/%s";
	String PUBLIC_API_SHAREDSPACE_LEVEL_ENTITIES = PUBLIC_API + "/shared_spaces/%s" + "/%s";

	String PUBLIC_API_WORKSPACE_FORMAT = PUBLIC_API_SHAREDSPACE_FORMAT + "/workspaces/%s";
	String PUBLIC_API_WORKSPACE_LEVEL_ENTITIES = PUBLIC_API_WORKSPACE_FORMAT + "/%s";


}
