package com.microfocus.octane.plugins.configuration;

public class PluginConstants {

    public static final int SPACE_CONTEXT = -1;
    public static final String PASSWORD_REPLACE = "__secret__password__"; // NON-NLS

    public static String URL_AUTHENTICATION = "/authentication/sign_in";

    public static String PUBLIC_API = "/api";
    public static String PUBLIC_API_SHAREDSPACE_FORMAT = PUBLIC_API + "/shared_spaces/%s";
    public static String PUBLIC_API_SHAREDSPACE_LEVEL_ENTITIES = PUBLIC_API + "/shared_spaces/%s" + "/%s";

    public static String PUBLIC_API_WORKSPACE_FORMAT = PUBLIC_API_SHAREDSPACE_FORMAT + "/workspaces/%s";
    public static String PUBLIC_API_WORKSPACE_LEVEL_ENTITIES = PUBLIC_API_WORKSPACE_FORMAT + "/%s";
}
