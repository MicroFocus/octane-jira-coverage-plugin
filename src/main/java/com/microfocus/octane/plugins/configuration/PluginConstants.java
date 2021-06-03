package com.microfocus.octane.plugins.configuration;

public class PluginConstants {

    public static final int SPACE_CONTEXT = -1;
    public static final String PASSWORD_REPLACE = "__secret__password__"; // NON-NLS
    public static final String PATH = "path";

    public static String URL_AUTHENTICATION = "/authentication/sign_in";

    public static String PUBLIC_API = "/api";
    public static String PUBLIC_API_SHAREDSPACE_FORMAT = PUBLIC_API + "/shared_spaces/%s";
    public static String PUBLIC_API_SHAREDSPACE_LEVEL_ENTITIES = PUBLIC_API + "/shared_spaces/%s" + "/%s";

    public static String PUBLIC_API_WORKSPACE_FORMAT = PUBLIC_API_SHAREDSPACE_FORMAT + "/workspaces/%s";
    public static String PUBLIC_API_WORKSPACE_LEVEL_ENTITIES = PUBLIC_API_WORKSPACE_FORMAT + "/%s";

    //octane version
    public static final String DEFAULT_BUILD = "9999";
    public static final String SEPARATOR = ".";
    public static final String FUGEES_VERSION = "15.1.90";
    public static final String GUNSNROSES_PUSH2 = "16.0.15";
}
