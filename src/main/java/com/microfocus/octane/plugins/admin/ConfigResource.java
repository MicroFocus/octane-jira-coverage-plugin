/*
 *     Copyright 2018 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.microfocus.octane.plugins.admin;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.microfocus.octane.plugins.components.api.Constants;
import com.microfocus.octane.plugins.components.api.OctaneRestService;
import com.microfocus.octane.plugins.configuration.LocationParts;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;
import com.microfocus.octane.plugins.configuration.SpaceConfiguration;
import com.microfocus.octane.plugins.configuration.WorkspaceConfiguration;
import com.microfocus.octane.plugins.rest.OctaneEntityParser;
import com.microfocus.octane.plugins.rest.RestConnector;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.query.OctaneQueryBuilder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.stream.Collectors;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
@Scanned
public class ConfigResource {

    private static final Logger log = LoggerFactory.getLogger(ConfigResource.class);

    @ComponentImport
    private final UserManager userManager;

    private final OctaneRestService octaneRestService;

    @Inject
    public ConfigResource(UserManager userManager, OctaneRestService octaneRestService) {
        this.userManager = userManager;
        this.octaneRestService = octaneRestService;
    }


    @GET
    @Path("/workspace-config/additional-data")
    public Response getDataForCreateDialog(@Context HttpServletRequest request, @QueryParam("update-workspace-id") Long id) {
        if (!hasPermissions(request)) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        SpaceConfiguration spConfig = OctaneConfigurationManager.getInstance().getConfiguration();
        Set<Long> usedWorkspaces = spConfig.getWorkspaces().stream().map(WorkspaceConfiguration::getWorkspaceId).collect(Collectors.toSet());
        Set<String> usedJiraProjects = spConfig.getWorkspaces().stream().flatMap(c -> c.getJiraProjects().stream()).collect(Collectors.toSet());

        if (id != null) {
            Optional<WorkspaceConfiguration> opt = spConfig.getWorkspaces().stream().filter(wc -> wc.getWorkspaceId() == id).findFirst();
            if (opt.isPresent()) {
                usedWorkspaces.remove(opt.get().getWorkspaceId());
                usedJiraProjects.removeAll(opt.get().getJiraProjects());
            }
        }

        OctaneEntityCollection workspaces = octaneRestService.getEntitiesByCondition(OctaneRestService.SPACE_CONTEXT, "workspaces", null, Arrays.asList("id", "name"));
        Collection<Select2ResultItem> select2workspaces = workspaces.getData()
                .stream()
                .filter(e -> !usedWorkspaces.contains(Long.valueOf(e.getId())))
                .map(e -> new Select2ResultItem(e.getId(), e.getName()))
                .collect(Collectors.toList());


        Collection<Select2ResultItem> select2IssueTypes = ComponentAccessor.getConstantsManager().getAllIssueTypeObjects()
                .stream().map(e -> new Select2ResultItem(e.getName(), e.getName())).sorted(Comparator.comparing(o -> o.getId())).collect(Collectors.toList());

        Collection<Select2ResultItem> select2Projects = ComponentAccessor.getProjectManager().getProjectObjects()
                .stream()
                .filter(e -> !usedJiraProjects.contains(e.getKey()))
                .map(e -> new Select2ResultItem(e.getKey(), e.getKey())).sorted(Comparator.comparing(o -> o.getId()))
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("workspaces", select2workspaces);
        data.put("issueTypes", select2IssueTypes);
        data.put("projects", select2Projects);

        return Response.ok(data).build();
    }

    @GET
    @Path("/workspace-config/all")
    public Response getAllWorkspaceConfigurations(@Context HttpServletRequest request) {
        if (!hasPermissions(request)) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        Collection<WorkspaceConfigurationOutgoing> result = OctaneConfigurationManager.getInstance().getConfiguration()
                .getWorkspaces().stream().map(wc -> convert(wc))
                .sorted(Comparator.comparing(WorkspaceConfigurationOutgoing::getWorkspaceName))
                .collect(Collectors.toList());

        return Response.ok(result).build();
    }

    private WorkspaceConfigurationOutgoing convert(WorkspaceConfiguration wc) {
        WorkspaceConfigurationOutgoing result = new WorkspaceConfigurationOutgoing()
                .setId(wc.getWorkspaceId())
                .setWorkspaceId(wc.getWorkspaceId())
                .setWorkspaceName(wc.getWorkspaceName())
                .setOctaneUdf(wc.getOctaneUdf())
                .setOctaneEntityTypes(wc.getOctaneEntityTypes())
                .setJiraIssueTypes(wc.getJiraIssueTypes())
                .setJiraProjects(wc.getJiraProjects());

        return result;
    }

    @GET
    @Path("/workspace-config/self/{id}")
    public Response getWorkspaceConfigurationById(@Context HttpServletRequest request, @PathParam("id") long id) {
        if (!hasPermissions(request)) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        Optional<WorkspaceConfigurationOutgoing> optResult = OctaneConfigurationManager.getInstance().getConfiguration()
                .getWorkspaces().stream().filter(wc -> wc.getWorkspaceId() == id).map(wc -> convert(wc)).findFirst();

        if (optResult.isPresent()) {
            return Response.ok(optResult.get()).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/workspace-config/supported-octane-types")
    public Response getSupportedOctaneTypes(@Context HttpServletRequest request, @QueryParam("workspace-id") long workspaceId, @QueryParam("udf-name") String udfName) {
        if (!hasPermissions(request)) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        List<String> types = octaneRestService.getSupportedOctaneTypes(workspaceId, udfName);
        return Response.ok(types).build();
    }

    /*@PUT
    @Path("/workspace-config/self/{id}")
    public Response updateWorkspaceConfigurationById(@Context HttpServletRequest request, @PathParam("id") long id, WorkspaceConfigurationOutgoing modelForUpdate) {
        if (!hasPermissions(request)) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        WorkspaceConfiguration wc = OctaneConfigurationManager.getInstance().saveWorkspaceConfiguration(modelForUpdate);
        return Response.ok(convert(wc)).build();
    }*/

    @POST
    @Path("/workspace-config/self")
    public Response createWorkspaceConfiguration(@Context HttpServletRequest request, WorkspaceConfigurationOutgoing model) {
        if (!hasPermissions(request)) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        WorkspaceConfiguration wc = OctaneConfigurationManager.getInstance().saveWorkspaceConfiguration(model);
        return Response.ok(convert(wc)).build();
    }

    @DELETE
    @Path("/workspace-config/self/{id}")
    public Response deleteWorkspaceConfigurationById(@Context HttpServletRequest request, @PathParam("id") long id) {
        if (!hasPermissions(request)) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        boolean deleted = OctaneConfigurationManager.getInstance().deleteWorkspaceConfiguration(id);
        if (deleted) {
            return Response.ok().build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }

    }

    private boolean hasPermissions(HttpServletRequest request) {
        UserProfile username = userManager.getRemoteUser(request);
        return (username != null && userManager.isSystemAdmin(username.getUserKey()));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context HttpServletRequest request) {
        if (!hasPermissions(request)) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        SpaceConfiguration spaceConfig = OctaneConfigurationManager.getInstance().getConfiguration();
        SpaceConfigurationOutgoing outgoing = SpaceConfigurationOutgoing
                .create(spaceConfig.getId(), spaceConfig.getLocation(), spaceConfig.getClientId(), OctaneConfigurationManager.PASSWORD_REPLACE);
        return Response.ok(outgoing).build();
    }


    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(final SpaceConfigurationOutgoing spaceModel, @Context HttpServletRequest request) {
        UserProfile username = userManager.getRemoteUser(request);
        if (username == null || !userManager.isSystemAdmin(username.getUserKey())) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        String errorMsg = checkConfiguration(spaceModel);

        if (errorMsg != null) {
            return Response.status(Status.CONFLICT).entity("Failed to save : " + errorMsg).build();
        } else {
            OctaneConfigurationManager.getInstance().saveSpaceConfiguration(spaceModel);
        }

        return Response.ok().build();
    }

    private String checkConfiguration(SpaceConfigurationOutgoing spaceModel) {
        String errorMsg = null;
        if (StringUtils.isEmpty(spaceModel.getLocation())) {
            errorMsg = "Location URL is required";
        } else if (StringUtils.isEmpty(spaceModel.getClientId())) {
            errorMsg = "Client ID is required";
        } else if (StringUtils.isEmpty(spaceModel.getClientSecret())) {
            errorMsg = "Client secret is required";
        } else {
            LocationParts locationParts = null;
            try {
                locationParts = OctaneConfigurationManager.parseUiLocation(spaceModel.getLocation());
            } catch (Exception e) {
                errorMsg = e.getMessage();
            }

            if (errorMsg == null) {
                try {

                    String secret = OctaneConfigurationManager.PASSWORD_REPLACE.equals(spaceModel.getClientSecret()) ?
                            OctaneConfigurationManager.getInstance().getConfiguration().getClientSecret() :
                            spaceModel.getClientSecret();

                    RestConnector restConnector = new RestConnector();
                    restConnector.setBaseUrl(locationParts.getBaseUrl());
                    restConnector.setCredentials(spaceModel.getClientId(), secret);
                    boolean isConnected = restConnector.login();
                    if (!isConnected) {
                        errorMsg = "Failed to authenticate";
                    } else {
                        String getWorspacesUrl = String.format(Constants.PUBLIC_API_SHAREDSPACE_LEVEL_ENTITIES, locationParts.getSpaceId(), "workspaces");
                        String queryString = OctaneQueryBuilder.create().addSelectedFields("id").addPageSize(1).build();
                        Map<String, String> headers = new HashMap<>();
                        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

                        try {
                            String entitiesCollectionStr = restConnector.httpGet(getWorspacesUrl, Arrays.asList(queryString), headers).getResponseData();
                            JSONObject jsonObj = new JSONObject(entitiesCollectionStr);
                            OctaneEntityCollection workspaces = OctaneEntityParser.parseCollection(jsonObj);
                        } catch (JSONException e) {
                            errorMsg = "Incorrect sharedspace id";
                        }
                    }
                } catch (Exception exc) {
                    if (exc.getMessage().contains("platform.not_authorized")) {
                        errorMsg = "Validate credentials";
                    } else if (exc.getMessage().contains("type shared_space does not exist")) {
                        errorMsg = "Sharedspace '" + locationParts.getSpaceId() + "' is not available";
                    } else {
                        errorMsg = "Unexpected " + exc.getClass().getName() + " : " + exc.getMessage();//"Validate that location is correct.";
                    }
                }
            }
        }
        return errorMsg;
    }
}
