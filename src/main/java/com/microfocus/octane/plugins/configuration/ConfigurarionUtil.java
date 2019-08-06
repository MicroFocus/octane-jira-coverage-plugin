package com.microfocus.octane.plugins.configuration;

import com.microfocus.octane.plugins.admin.SpaceConfigurationOutgoing;
import com.microfocus.octane.plugins.admin.WorkspaceConfigurationOutgoing;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeDescriptor;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeManager;
import com.microfocus.octane.plugins.rest.RestStatusException;
import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigurarionUtil {

    private static final String PARAM_SHARED_SPACE = "p"; // NON-NLS

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

    public static SpaceConfiguration validateRequiredAndConvertToInternal(SpaceConfigurationOutgoing sco, boolean isNew) {

        if (StringUtils.isEmpty(sco.getLocation())) {
            throw new IllegalArgumentException("Location URL is required");
        }
        if (StringUtils.isEmpty(sco.getClientId())) {
            throw new IllegalArgumentException("Client ID is required");
        }
        if (StringUtils.isEmpty(sco.getClientSecret())) {
            throw new IllegalArgumentException("Client secret is required");
        }

        LocationParts locationParts = null;
        try {
            locationParts = parseUiLocation(sco.getLocation());
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }

            /*String errorMsg = null;
            try {

                String secret = PluginConstants.PASSWORD_REPLACE.equals(sco.getClientSecret()) ?
                        ConfigurationManager.getInstance().getSpaceConfigurationById(sco.getId()).get().getClientSecret() :
                        sco.getClientSecret();

                RestConnector restConnector = new RestConnector();
                restConnector.setBaseUrl(locationParts.getBaseUrl());
                restConnector.setCredentials(sco.getClientId(), secret);
                boolean isConnected = restConnector.login();
                if (!isConnected) {
                    errorMsg = "Failed to authenticate.";
                } else {
                    String getWorspacesUrl = String.format(PluginConstants.PUBLIC_API_SHAREDSPACE_LEVEL_ENTITIES, locationParts.getSpaceId(), "workspaces");
                    String queryString = OctaneQueryBuilder.create().addSelectedFields("id").addPageSize(1).build();
                    Map<String, String> headers = new HashMap<>();
                    headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

                    try {
                        String entitiesCollectionStr = restConnector.httpGet(getWorspacesUrl, Arrays.asList(queryString), headers).getResponseData();
                        JSONObject jsonObj = new JSONObject(entitiesCollectionStr);
                        OctaneEntityCollection workspaces = OctaneEntityParser.parseCollection(jsonObj);
                    } catch (JSONException e) {
                        errorMsg = "Incorrect shared space ID.";
                    }
                }
            } catch (Exception exc) {
                if (exc.getMessage().contains("platform.not_authorized")) {
                    errorMsg = "Ensure your credentials are correct.";
                } else if (exc.getMessage().contains("type shared_space does not exist") || exc.getMessage().contains("SharedSpaceNotFoundException")) {
                    errorMsg = "Shared space '" + locationParts.getSpaceId() + "' does not exist.";
                } else if (exc.getCause() != null && exc.getCause() instanceof SSLHandshakeException && exc.getCause().getMessage().contains("Received fatal alert")) {
                    errorMsg = "Network exception, proxy settings may be missing.";
                } else if (exc.getMessage().startsWith("Connection timed out")) {
                    errorMsg = "Timed out exception, proxy settings may be misconfigured.";
                } else if (exc.getCause() != null && exc.getCause() instanceof UnknownHostException) {
                    errorMsg = "Location is not available.";
                } else {
                    errorMsg = "Exception " + exc.getClass().getName() + " : " + exc.getMessage();
                    if (exc.getCause() != null) {
                        errorMsg += " . Cause : " + exc.getCause();//"Validate that location is correct.";
                    }
                }
            }
            if (errorMsg != null) {
                throw new IllegalArgumentException(errorMsg);
            }*/

        //validate id
        if (isNew) {
            if (StringUtils.isNotEmpty(sco.getId())) {
                throw new IllegalArgumentException("New space configuration cannot contain configuration id");
            }
            sco.setId(UUID.randomUUID().toString());
        } else {
            if (StringUtils.isEmpty(sco.getId())) {
                throw new IllegalArgumentException("Configuration id is missing");
            }


        }

        //replace password if required
        String clientSecret = sco.getClientSecret();
        Optional<SpaceConfiguration> opt = ConfigurationManager.getInstance().getSpaceConfigurationById(sco.getId(), true);
        if (PluginConstants.PASSWORD_REPLACE.equals(clientSecret) && !isNew) {
            clientSecret = opt.get().getClientSecret();
        }

        //convert
        SpaceConfiguration sc = new SpaceConfiguration()
                .setId(sco.getId())
                .setName(sco.getName())
                .setLocation(sco.getLocation())
                .setLocationParts(locationParts)
                .setClientId(sco.getClientId())
                .setClientSecret(clientSecret);
        return sc;
    }

    public static SpaceConfigurationOutgoing convertToOutgoing(SpaceConfiguration sc) {
        SpaceConfigurationOutgoing sco = new SpaceConfigurationOutgoing()
                .setId(sc.getId())
                .setName(sc.getName())
                .setLocation(sc.getLocation())
                .setClientSecret(PluginConstants.PASSWORD_REPLACE)
                .setClientId(sc.getClientId());
        return sco;
    }

    public static void doFullSpaceConfigurationValidation(SpaceConfiguration spaceConfiguration) {
        validateSpaceNameIsUnique(spaceConfiguration);
        validateSpaceUrlIsUnique(spaceConfiguration);
        validateSpaceConfigurationConnectivity(spaceConfiguration);
    }

    private static void validateSpaceNameIsUnique(SpaceConfiguration spaceConfiguration) {
        Optional<SpaceConfiguration> opt = ConfigurationManager.getInstance().getSpaceConfigurations().stream()
                .filter((s -> !s.getId().equals(spaceConfiguration.getId()) //don't check the same configuration
                        && s.getName().equals(spaceConfiguration.getName())))
                .findFirst();

        if (opt.isPresent()) {
            String msg = String.format("Name '%s' is already in use by another space configuration.", spaceConfiguration.getName());
            throw new IllegalArgumentException(msg);
        }
    }

    private static void validateSpaceUrlIsUnique(SpaceConfiguration spaceConfiguration) {
        Optional<SpaceConfiguration> opt = ConfigurationManager.getInstance().getSpaceConfigurations().stream()
                .filter((s -> !s.getId().equals(spaceConfiguration.getId()) //don't check the same configuration
                        && s.getLocationParts().getKey().equals(spaceConfiguration.getLocationParts().getKey())))
                .findFirst();

        if (opt.isPresent()) {
            String msg = String.format("Space location is already defined in space configuration '%s'", opt.get().getName());
            throw new IllegalArgumentException(msg);
        }
    }

    public static void validateSpaceConfigurationConnectivity(SpaceConfiguration spaceConfig) {
        try {
            OctaneRestManager.getWorkspaces(spaceConfig);
        } catch (RestStatusException e) {
            if (e.getStatus() == 404 && e.getMessage().contains("SharedSpaceNotFoundException")) {
                throw new IllegalArgumentException(String.format("Space id '%d' is not exist", spaceConfig.getLocationParts().getSpaceId()));
            } else {
                throw e;
            }
        }
    }


    public static WorkspaceConfigurationOutgoing convertToOutgoing(WorkspaceConfiguration wc, Map<String, String> spaceConfigurationId2Name) {
        WorkspaceConfigurationOutgoing result = new WorkspaceConfigurationOutgoing()
                .setId(wc.getId())
                .setSpaceConfigId(wc.getSpaceConfigurationId())
                .setSpaceConfigName(spaceConfigurationId2Name.get(wc.getSpaceConfigurationId()))
                .setWorkspaceId(Long.toString(wc.getWorkspaceId()))
                .setWorkspaceName(wc.getWorkspaceName())
                .setOctaneUdf(wc.getOctaneUdf())
                .setOctaneEntityTypes(wc.getOctaneEntityTypes().stream()
                        .map(typeName -> {
                            OctaneEntityTypeDescriptor desc = OctaneEntityTypeManager.getByTypeName(typeName);
                            return desc == null ? "" : desc.getLabel();
                        })
                        .sorted().collect(Collectors.toList()))
                .setJiraIssueTypes(wc.getJiraIssueTypes())
                .setJiraProjects(wc.getJiraProjects());

        return result;
    }

    public static WorkspaceConfiguration validateRequiredAndConvertToInternal(WorkspaceConfigurationOutgoing wco, boolean isNew) {
        //validation

        if (StringUtils.isEmpty(wco.getSpaceConfigId())) {
            throw new IllegalArgumentException("Space configuration is missing.");
        }

        if (StringUtils.isEmpty(wco.getWorkspaceId())) {
            throw new IllegalArgumentException("Workspace id is missing.");
        }

        if (StringUtils.isEmpty(wco.getWorkspaceName())) {
            throw new IllegalArgumentException("Workspace id is missing.");
        }

        if (wco.getOctaneEntityTypes().size() == 0) {
            throw new IllegalArgumentException("Octane entity types are missing");
        }
        if (wco.getJiraProjects().size() == 0) {
            throw new IllegalArgumentException("Jira projects are missing");
        }
        if (wco.getJiraIssueTypes().size() == 0) {
            throw new IllegalArgumentException("Jira issue types are missing");
        }
        long workspaceId;
        try {
            workspaceId = Long.parseLong(wco.getWorkspaceId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Workspace Id must be numeric value");
        }

        if (isNew) {
            if (StringUtils.isNotEmpty(wco.getId())) {
                throw new IllegalArgumentException("New workspace configuration cannot contain configuration id");
            }
            wco.setId(UUID.randomUUID().toString());
        } else {
            if (StringUtils.isEmpty(wco.getId())) {
                throw new IllegalArgumentException("Configuration id is missing");
            }
        }

        WorkspaceConfiguration wc = new WorkspaceConfiguration()
                .setSpaceConfigurationId(wco.getSpaceConfigId())
                .setWorkspaceId(workspaceId)
                .setWorkspaceName(wco.getWorkspaceName())
                .setOctaneUdf(wco.getOctaneUdf())
                .setOctaneEntityTypes(wco.getOctaneEntityTypes().stream()
                        .map(label -> OctaneEntityTypeManager.getByLabel(label).getTypeName())
                        .collect(Collectors.toList()))
                .setJiraIssueTypes(wco.getJiraIssueTypes().stream().sorted().collect(Collectors.toList()))
                .setJiraProjects(wco.getJiraProjects().stream().sorted().collect(Collectors.toList()));

        return wc;
    }
}

