/*******************************************************************************
 * Copyright 2017-2023 Open Text.
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.microfocus.octane.plugins.configuration;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueConstant;
import com.microfocus.octane.plugins.admin.KeyValueItem;
import com.microfocus.octane.plugins.admin.SpaceConfigurationOutgoing;
import com.microfocus.octane.plugins.admin.WorkspaceConfigurationOutgoing;
import com.microfocus.octane.plugins.configuration.v3.JiraConfigGrouping;
import com.microfocus.octane.plugins.configuration.v3.OctaneConfigGrouping;
import com.microfocus.octane.plugins.configuration.v3.SpaceConfiguration;
import com.microfocus.octane.plugins.configuration.v3.WorkspaceConfiguration;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeDescriptor;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeManager;
import com.microfocus.octane.plugins.rest.RestStatusException;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.query.LogicalQueryPhrase;
import com.microfocus.octane.plugins.rest.query.QueryPhrase;
import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

public class ConfigurationUtil {

    private static final String PARAM_SHARED_SPACE = "p"; // NON-NLS
    private static final String OCTANE_WS_SEPARATOR = " - ";

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
        if (StringUtils.isEmpty(sco.getName().trim())) {
            throw new IllegalArgumentException("Space configuration name is required");
        }

        if (sco.getName().trim().length() > 40) {
            throw new IllegalArgumentException("Space configuration name exceeds allowed length (40 characters)");
        }
    }

    public static SpaceConfiguration validateRequiredAndConvertToInternal(SpaceConfigurationOutgoing sco, boolean isNew) {

        if (StringUtils.isEmpty(sco.getLocation().trim())) {
            throw new IllegalArgumentException("Location URL is required");
        }
        if (StringUtils.isEmpty(sco.getClientId().trim())) {
            throw new IllegalArgumentException("Client ID is required");
        }
        if (StringUtils.isEmpty(sco.getClientSecret())) {
            throw new IllegalArgumentException("Client secret is required");
        }

        LocationParts locationParts = null;
        try {
            locationParts = parseUiLocation(sco.getLocation().trim());
            sco.setLocation(locationParts.getKey()); //remove from url what's after sharedspace id
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
        SpaceConfiguration sc = new SpaceConfiguration(
                sco.getName().trim(),
                sco.getLocation().trim(),
                locationParts,
                sco.getClientId().trim(),
                clientSecret,
                sco.getId());

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

    public static void doSpaceConfigurationUniquenessValidation(SpaceConfiguration spaceConfiguration, boolean isConnectionTested) {
        try {
            validateSpaceNameIsUnique(spaceConfiguration);
        } catch (IllegalArgumentException ex) {
            if (isConnectionTested) {
                throw new IllegalArgumentException("Connection is successful, but the following problem was found: " + ex.getMessage());
            } else {
                throw ex;
            }
        }
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

    public static void validateSpaceConfigurationConnectivity(SpaceConfiguration spaceConfig) {
        try {
            OctaneRestManager.getWorkspaces(spaceConfig);
        } catch (RestStatusException e) {
            String msg = parseExceptionMessage(e, spaceConfig);
            throw new IllegalArgumentException(msg);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Test connection failed: " + e.getMessage()); //rethrow IllegalArgumentExceptions, so it can catch the Runtime ones
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Test connection failed: Error occurred while trying to test the connection. Please check the host.");
        } catch (Exception e) {
            throw new IllegalArgumentException("Test connection failed: " + e.getMessage());
        }
    }

    public static String parseExceptionMessage(RestStatusException e, SpaceConfiguration spaceConfig) {
        if (e.getStatus() == 404 && e.getMessage().contains("SharedSpaceNotFoundException")) {
            return String.format("Space id '%d' does not exist", spaceConfig.getLocationParts().getSpaceId());
        } else {
            return "Test connection failed: " + e.getMessage();
        }
    }

    public static WorkspaceConfigurationOutgoing convertToOutgoing(WorkspaceConfiguration wc, Map<String, String> spaceConfigurationId2Name) {
        return new WorkspaceConfigurationOutgoing(
                wc.getId(),
                wc.getSpaceConfigurationId(),
                spaceConfigurationId2Name.get(wc.getSpaceConfigurationId()),
                wc.getOctaneConfigGrouping().getOctaneWorkspaces().stream()
                        .map(ows -> ows.getId() + " - " + ows.getName())
                        .collect(Collectors.toSet()),
                wc.getOctaneConfigGrouping().getOctaneUdf(),
                wc.getOctaneConfigGrouping().getOctaneEntityTypes().stream()
                        .map(typeName -> {
                            OctaneEntityTypeDescriptor desc = OctaneEntityTypeManager.getByTypeName(typeName);
                            return desc == null ? "" : desc.getLabel();
                        })
                        .collect(Collectors.toSet()),
                wc.getJiraConfigGrouping().getIssueTypes(),
                wc.getJiraConfigGrouping().getProjectNames());
    }

    public static WorkspaceConfiguration validateRequiredAndConvertToInternal(WorkspaceConfigurationOutgoing wco, boolean isNew) {
        //validation

        if (StringUtils.isEmpty(wco.getSpaceConfigId())) {
            throw new IllegalArgumentException("Space configuration is missing.");
        }

        if (isEmpty(wco.getWorkspaces())) {
            throw new IllegalArgumentException("Workspaces field is empty.");
        }

        if (isEmpty(wco.getJiraProjects())) {
            throw new IllegalArgumentException("Jira projects are missing");
        }
        if (isEmpty(wco.getJiraIssueTypes())) {
            throw new IllegalArgumentException("Jira issue types are missing");
        }

        try {
            wco.getWorkspaces().stream()
                    .map(ws -> Long.parseLong(ws.split(OCTANE_WS_SEPARATOR)[0]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Workspace Ids must be numeric value");
        }

        Set<OctaneWorkspace> octaneWorkspaces = wco.getWorkspaces().stream()
                .map(ws -> {
                    String[] wsParts = ws.split(OCTANE_WS_SEPARATOR, 2);

                    if (wsParts.length < 2) {
                        throw new IllegalArgumentException("Workspaces must have the following pattern: 'workspaceId - workspaceName'");
                    }

                    return new OctaneWorkspace(wsParts[0], wsParts[1]);
                })
                .collect(Collectors.toSet());

        SpaceConfiguration spaceConfiguration = ConfigurationManager.getInstance().getSpaceConfigurationById(wco.getSpaceConfigId(), true).get();
        validateSpaceConfigurationConnectivity(spaceConfiguration);

        validateWorkspaces(octaneWorkspaces, wco.getSpaceConfigId());
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

        List<Set<String>> octaneWorkspacesEntityTypes = octaneWorkspaces.stream()
                .map(OctaneWorkspace::getId)
                .map(wsId -> ConfigurationUtil.getOctaneTypesList(wco.getSpaceConfigId(), wco.getOctaneUdf(), wsId))
                .collect(Collectors.toList());

        Set<String> commonOctaneEntityTypes = retainAllSets(octaneWorkspacesEntityTypes);

        if (isEmpty(commonOctaneEntityTypes)) {
            throw new IllegalArgumentException("There are zero Octane entity types found for the given workspaces and udf");
        } else {
            if (!isNull(wco.getOctaneEntityTypes()) && !commonOctaneEntityTypes.equals(wco.getOctaneEntityTypes())) {
                throw new IllegalArgumentException("The Octane entity types provided does not match with the ones that are " +
                        "found in Octane at the moment. Please review them once again and adapt the configuration properly " +
                        "or remove the octaneEntityTypes field to autofill them with the right entity types (the ones which contains your udf)");
            }
        }

        return new WorkspaceConfiguration(
                wco.getId(),
                wco.getSpaceConfigId(),
                new OctaneConfigGrouping(octaneWorkspaces,
                        wco.getOctaneUdf(),
                        commonOctaneEntityTypes.stream()
                                .map(entityType -> OctaneEntityTypeManager.getByLabel(entityType).getTypeName())
                                .collect(Collectors.toSet())
                ),
                new JiraConfigGrouping(wco.getJiraProjects(), wco.getJiraIssueTypes())
        );
    }

    public static Set<String> retainAllSets(List<Set<String>> octaneWorkspacesEntityTypes) {
        Set<String> commonOctaneEntityTypes = octaneWorkspacesEntityTypes.get(0);

        octaneWorkspacesEntityTypes.forEach(commonOctaneEntityTypes::retainAll);

        return commonOctaneEntityTypes;
    }

    private static void validateWorkspaces(Set<OctaneWorkspace> octaneWorkspaces, String spaceConfigId) {
        List<String> validWorkspaceIds = getValidWorkspaces(spaceConfigId).stream()
                .map(KeyValueItem::getId)
                .collect(Collectors.toList());

        if (!validWorkspaceIds.containsAll(octaneWorkspaces.stream()
                .map(OctaneWorkspace::getId)
                .collect(Collectors.toList()))) {
            throw new IllegalArgumentException("One of the selected workspace is not valid. Either it doesn't exist, it is not reachable/active");
        }
    }

    private static void validateJiraProjectKey(WorkspaceConfigurationOutgoing wco) {
        Collection<KeyValueItem> validProjects = getValidProjectsMap();

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

    public static Set<String> getOctaneTypesList(String spaceConfigId, String octaneUdf, String workspaceId) {
        SpaceConfiguration sc = ConfigurationManager.getInstance().getSpaceConfigurationById(spaceConfigId, true).get();

        Set<String> octaneEntityTypes = OctaneRestManager.getSupportedOctaneTypes(sc, Long.parseLong(workspaceId), octaneUdf);

        return octaneEntityTypes
                .stream()
                .map(t -> OctaneEntityTypeManager.getByTypeName(t).getLabel())
                .collect(Collectors.toSet());
    }

    public static Collection<KeyValueItem> getValidWorkspaces(String spaceConfigId) {
        SpaceConfiguration spConfig = ConfigurationManager.getInstance().getSpaceConfigurationById(spaceConfigId, true).get();

        List<QueryPhrase> conditions = Arrays.asList(new LogicalQueryPhrase("activity_level", 0));//only active workspaces

        int limit = 500, offset = 0;

        OctaneEntityCollection workspaces = OctaneRestManager.getEntitiesByCondition(spConfig, PluginConstants.SPACE_CONTEXT, "workspaces", conditions, Arrays.asList("id", "name"), limit, null);

        //repeat the call if the total count is higher than the limit (because of the pagination)
        while (workspaces.getTotalCount() > workspaces.getData().size()) {
            offset += limit;

            OctaneEntityCollection nextPageOfWorkspaces = OctaneRestManager.getEntitiesByCondition(spConfig, PluginConstants.SPACE_CONTEXT, "workspaces", conditions, Arrays.asList("id", "name"), limit, offset);

            List<OctaneEntity> newWorkspacesDataList = workspaces.getData();
            newWorkspacesDataList.addAll(nextPageOfWorkspaces.getData());

            workspaces.setData(newWorkspacesDataList);
        }

        return workspaces.getData()
                .stream()
                .map(e -> new KeyValueItem(e.getId(), e.getName()))
                .collect(Collectors.toList());
    }

    public static Collection<KeyValueItem> getValidProjectsMap() {
        return ComponentAccessor.getProjectManager().getProjectObjects()
                .stream()
                .map(e -> new KeyValueItem(e.getKey(), e.getKey()))
                .sorted(Comparator.comparing(KeyValueItem::getId))
                .collect(Collectors.toList());
    }
}