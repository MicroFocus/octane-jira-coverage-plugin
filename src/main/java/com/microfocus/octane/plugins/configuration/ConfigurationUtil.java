package com.microfocus.octane.plugins.configuration;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.project.Project;
import com.microfocus.octane.plugins.admin.KeyValueItem;
import com.microfocus.octane.plugins.admin.SpaceConfigurationOutgoing;
import com.microfocus.octane.plugins.admin.WorkspaceConfigurationOutgoing;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeDescriptor;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeManager;
import com.microfocus.octane.plugins.rest.RestStatusException;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.query.LogicalQueryPhrase;
import com.microfocus.octane.plugins.rest.query.QueryPhrase;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.QueryParam;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigurationUtil {

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

    public static void validateName(SpaceConfigurationOutgoing sco) {
        if (StringUtils.isEmpty(sco.getName())) {
            throw new IllegalArgumentException("Space configuration name is required");
        }

        if (sco.getName().length() > 40) {
            throw new IllegalArgumentException("Space configuration name exceeds allowed length (40 characters)");
        }
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


        String clientSecret = sco.getClientSecret();
        if (isNew) {
            //validate id is missing
            if (StringUtils.isNotEmpty(sco.getId())) {
                throw new IllegalArgumentException("New space configuration cannot contain configuration id");
            }
            sco.setId(UUID.randomUUID().toString());
        } else {
            //validate id is exist
            if (StringUtils.isEmpty(sco.getId())) {
                throw new IllegalArgumentException("Configuration id is missing");
            }

            //replace password if required
            Optional<SpaceConfiguration> opt = ConfigurationManager.getInstance().getSpaceConfigurationById(sco.getId(), true);
            if (PluginConstants.PASSWORD_REPLACE.equals(clientSecret) && !isNew) {
                clientSecret = opt.get().getClientSecret();
            }
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

    public static void doSpaceConfigurationUniquenessValidation(SpaceConfiguration spaceConfiguration) {
        validateSpaceNameIsUnique(spaceConfiguration);
        validateSpaceUrlIsUnique(spaceConfiguration);
        //validateSpaceConfigurationConnectivity(spaceConfiguration);
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
            throw tryTranslateException(e,spaceConfig);
        }
    }

    public static RuntimeException tryTranslateException(RestStatusException e, SpaceConfiguration spaceConfig){
        if (e.getStatus() == 404 && e.getMessage().contains("SharedSpaceNotFoundException")) {
            return new IllegalArgumentException(String.format("Space id '%d' is not exist", spaceConfig.getLocationParts().getSpaceId()));
        } else {
            return e;
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
                        .collect(Collectors.toSet()))
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

        validateWorkspace(wco);
        validateOctaneTypesList(wco, workspaceId);
        validateJiraIssuesList(wco);
        validateJiraProjectKey(wco);

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
                .setId(wco.getId())
                .setSpaceConfigurationId(wco.getSpaceConfigId())
                .setWorkspaceId(workspaceId)
                .setWorkspaceName(wco.getWorkspaceName())
                .setOctaneUdf(wco.getOctaneUdf())
                .setOctaneEntityTypes(wco.getOctaneEntityTypes().stream()
                        .map(label -> OctaneEntityTypeManager.getByLabel(label).getTypeName())
                        .collect(Collectors.toSet()))
                .setJiraIssueTypes(new HashSet<>(wco.getJiraIssueTypes()))
                .setJiraProjects(new HashSet<>(wco.getJiraProjects()));

        return wc;
    }

    private static void validateWorkspace(WorkspaceConfigurationOutgoing wco) {
        Collection<KeyValueItem> validWorkspaces = getValidWorkspaces(wco.getSpaceConfigId(), wco.getId());

        if (validWorkspaces.stream().noneMatch(e -> e.getText().equals(wco.getWorkspaceName()))) {
            throw new IllegalArgumentException("Workspace is not valid");
        }
    }

    private static void validateJiraProjectKey(WorkspaceConfigurationOutgoing wco) {
        Collection<KeyValueItem> validProjects = getValidProjects(wco.getId());

        if (!validProjects.stream()
                .map(KeyValueItem::getId)
                .collect(Collectors.toList())
                .containsAll(wco.getJiraProjects())) {
            throw new IllegalArgumentException("Jira projects list is not valid. The projects are either already used or don't exist");
        }
    }

    private static void validateJiraIssuesList(WorkspaceConfigurationOutgoing wco) {
        if (!ComponentAccessor.getConstantsManager().getAllIssueTypeObjects()
                .stream()
                .map(IssueConstant::getName)
                .collect(Collectors.toList())
                .containsAll(wco.getJiraIssueTypes())) {
            throw new IllegalArgumentException("Jira issue types list is not valid");
        }
    }

    private static void validateOctaneTypesList(WorkspaceConfigurationOutgoing wco, long workspaceId) {
        String spaceConfigurationId = wco.getSpaceConfigId();
        SpaceConfiguration sc = ConfigurationManager.getInstance().getSpaceConfigurationById(spaceConfigurationId, true).get();

        List<String> octaneEntityTypes = OctaneRestManager.getSupportedOctaneTypes(sc, workspaceId, wco.getOctaneUdf());
        Set<String> octaneEntityLabels = octaneEntityTypes
                .stream()
                .map(t -> OctaneEntityTypeManager.getByTypeName(t).getLabel())
                .collect(Collectors.toSet());

        Set<String> providedEntityTypes = wco.getOctaneEntityTypes();
        if (providedEntityTypes.stream().anyMatch(e -> !octaneEntityLabels.contains(e.trim()))) {
            throw new IllegalArgumentException("Octane entity types list is not valid for the given udf and workspace");
        }
    }

    public static Collection<KeyValueItem> getValidWorkspaces(String spaceConfigId, String workspaceConfId) {
        SpaceConfiguration spConfig = ConfigurationManager.getInstance().getSpaceConfigurationById(spaceConfigId, true).get();
        Set<Long> usedWorkspaces = getAlreadyUsedWorkspaces(workspaceConfId, spaceConfigId);

        List<QueryPhrase> conditions = Arrays.asList(new LogicalQueryPhrase("activity_level", 0));//only active workspaces

        OctaneEntityCollection workspaces = OctaneRestManager.getEntitiesByCondition(spConfig, PluginConstants.SPACE_CONTEXT, "workspaces", conditions, Arrays.asList("id", "name"));

        return workspaces.getData()
                .stream()
                .filter(e -> !usedWorkspaces.contains(Long.valueOf(e.getId())))
                .map(e -> new KeyValueItem(e.getId(), e.getName()))
                .collect(Collectors.toList());
    }

    private static Set<Long> getAlreadyUsedWorkspaces(String currentWsConfigId, String spaceConfigId) {
        Set<Long> usedWorkspaces = spaceConfigId == null ? new HashSet<>() : ConfigurationManager.getInstance().getWorkspaceConfigurations()
                .stream()
                .filter(w -> w.getSpaceConfigurationId().equals(spaceConfigId))
                .map(WorkspaceConfiguration::getWorkspaceId)
                .collect(Collectors.toSet());

        if (currentWsConfigId != null) {
            WorkspaceConfiguration wc = ConfigurationManager.getInstance().getWorkspaceConfigurationById(currentWsConfigId, true).get();
            usedWorkspaces.remove(wc.getWorkspaceId());
        }

        return usedWorkspaces;
    }


    public static Collection<KeyValueItem> getValidProjects(String workspaceConfId) {
        Set<String> usedJiraProjects = ConfigurationUtil.getAlreadyUsedJiraProjects(workspaceConfId);

        return ComponentAccessor.getProjectManager().getProjectObjects()
                .stream()
                .filter(e -> !usedJiraProjects.contains(e.getKey()))
                .map(e -> new KeyValueItem(e.getKey(), e.getKey())).sorted(Comparator.comparing(KeyValueItem::getId))
                .collect(Collectors.toList());
    }

    private static Set<String> getAlreadyUsedJiraProjects(String workspaceConfId) {
        Set<String> usedJiraProjects = ConfigurationManager.getInstance().getWorkspaceConfigurations()
                .stream()
                .flatMap(c -> c.getJiraProjects().stream())
                .collect(Collectors.toSet());

        if (workspaceConfId != null) {
            WorkspaceConfiguration wc = ConfigurationManager.getInstance().getWorkspaceConfigurationById(workspaceConfId, true).get();
            usedJiraProjects.removeAll(wc.getJiraProjects());
        }

        return usedJiraProjects;
    }
}