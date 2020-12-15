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
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.microfocus.octane.plugins.configuration.*;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeManager;
import com.microfocus.octane.plugins.rest.ProxyConfiguration;
import com.microfocus.octane.plugins.rest.RestStatusException;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.query.LogicalQueryPhrase;
import com.microfocus.octane.plugins.rest.query.QueryPhrase;
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

    @Context
    HttpServletRequest request;

    @ComponentImport
    private final UserManager userManager;

    @Inject
    public ConfigResource(UserManager userManager) {
        this.userManager = userManager;
    }

    @GET
    @Path("/workspaces-dialog/additional-data")
    public Response getDataForWorkspaceDialog(@QueryParam("space-conf-id") String spaceConfId, @QueryParam("workspace-conf-id") String workspaceConfId) {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        SpaceConfiguration spConfig = ConfigurationManager.getInstance().getSpaceConfigurationById(spaceConfId, true).get();
        Set<Long> usedWorkspaces = spaceConfId == null ? Collections.emptySet() : ConfigurationManager.getInstance().getWorkspaceConfigurations().stream()
                .filter(w -> w.getSpaceConfigurationId().equals(spaceConfId))
                .map(WorkspaceConfiguration::getWorkspaceId).collect(Collectors.toSet());
        Set<String> usedJiraProjects = ConfigurationManager.getInstance().getWorkspaceConfigurations().stream().flatMap(c -> c.getJiraProjects().stream()).collect(Collectors.toSet());

        if (workspaceConfId != null) {
            WorkspaceConfiguration wc = ConfigurationManager.getInstance().getWorkspaceConfigurationById(workspaceConfId, true).get();
            usedWorkspaces.remove(wc.getWorkspaceId());
            usedJiraProjects.removeAll(wc.getJiraProjects());
        }

        try {
            List<QueryPhrase> conditions = Arrays.asList(new LogicalQueryPhrase("activity_level", 0));//only active workspaces
            OctaneEntityCollection workspaces = OctaneRestManager.getEntitiesByCondition(spConfig, PluginConstants.SPACE_CONTEXT, "workspaces", conditions, Arrays.asList("id", "name"));
            Collection<KeyValueItem> select2workspaces = workspaces.getData()
                    .stream()
                    .filter(e -> !usedWorkspaces.contains(Long.valueOf(e.getId())))
                    .map(e -> new KeyValueItem(e.getId(), e.getName()))
                    .collect(Collectors.toList());

            Collection<KeyValueItem> select2IssueTypes = ComponentAccessor.getConstantsManager().getAllIssueTypeObjects()
                    .stream().map(e -> new KeyValueItem(e.getName(), e.getName())).sorted(Comparator.comparing(KeyValueItem::getId)).collect(Collectors.toList());

            Collection<KeyValueItem> select2Projects = ComponentAccessor.getProjectManager().getProjectObjects()
                    .stream()
                    .filter(e -> !usedJiraProjects.contains(e.getKey()))
                    .map(e -> new KeyValueItem(e.getKey(), e.getKey())).sorted(Comparator.comparing(KeyValueItem::getId))
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("workspaces", select2workspaces);
            data.put("issueTypes", select2IssueTypes);
            data.put("projects", select2Projects);

            return Response.ok(data).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (RestStatusException e) {
            RuntimeException translatedException = ConfigurationUtil.tryTranslateException(e, spConfig);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(translatedException.getMessage()).build();
        }
    }

    @GET
    @Path("/workspaces")
    public Response getAllWorkspaceConfigurations() {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }
        Collection<WorkspaceConfigurationOutgoing> result = ConfigurationManager.getInstance().getWorkspaceConfigurations()
                .stream().map(wc -> ConfigurationUtil.convertToOutgoing(wc, getSpaceConfigurationId2Name()))
                //.sorted((h1, h2) -> h1.getWorkspace().getText().compareTo(h2.getWorkspace().getText()))
                .collect(Collectors.toList());

        return Response.ok(result).build();
    }

    @GET
    @Path("/workspaces/{id}")
    public Response getWorkspaceConfigurationById(@PathParam("id") String id) {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        Optional<WorkspaceConfiguration> optResult = ConfigurationManager.getInstance().getWorkspaceConfigurationById(id, false);

        if (optResult.isPresent()) {
            return Response.ok(ConfigurationUtil.convertToOutgoing(optResult.get(), getSpaceConfigurationId2Name())).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/workspaces/supported-octane-types")
    public Response getSupportedOctaneTypes(@QueryParam("space-configuration-id") String spaceConfigurationId, @QueryParam("workspace-id") long workspaceId, @QueryParam("udf-name") String udfName) {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        SpaceConfiguration sc = ConfigurationManager.getInstance().getSpaceConfigurationById(spaceConfigurationId, true).get();
        List<String> types = OctaneRestManager.getSupportedOctaneTypes(sc, workspaceId, udfName);
        List<String> names = types.stream().map(t -> OctaneEntityTypeManager.getByTypeName(t).getLabel()).sorted().collect(Collectors.toList());
        return Response.ok(names).build();
    }

    @GET
    @Path("/workspaces/possible-jira-fields")
    public Response getPossibleJiraFields(@QueryParam("space-conf-id") String spaceConfigurationId, @QueryParam("workspace-id") long workspaceId) {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        SpaceConfiguration sc = ConfigurationManager.getInstance().getSpaceConfigurationById(spaceConfigurationId, true).get();
        Set<String> names = OctaneRestManager.getPossibleJiraFields(sc, workspaceId);
        return Response.ok(names).build();
    }

    @POST
    @Path("/workspaces")
    public Response addWorkspaceConfiguration(WorkspaceConfigurationOutgoing wco) {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        try {
            WorkspaceConfiguration wc = ConfigurationUtil.validateRequiredAndConvertToInternal(wco, true);
            wc = ConfigurationManager.getInstance().addWorkspaceConfiguration(wc);
            WorkspaceConfigurationOutgoing outputWco = ConfigurationUtil.convertToOutgoing(wc, getSpaceConfigurationId2Name());
            return Response.ok(outputWco).build();
        } catch (Exception e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("/workspaces/{workspaceConfigurationId}")
    public Response updateWorkspaceConfiguration(@PathParam("workspaceConfigurationId") String workspaceConfigurationId, WorkspaceConfigurationOutgoing wco) {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        try {
            WorkspaceConfiguration wc = ConfigurationUtil.validateRequiredAndConvertToInternal(wco, false);
            if (!wco.getId().equals(workspaceConfigurationId)) {
                return Response.status(Response.Status.CONFLICT).entity("Workspace configuration id in entity should be equal to id in path parameter").build();
            }
            WorkspaceConfiguration updatedWc = ConfigurationManager.getInstance().updateWorkspaceConfiguration(wc);
            WorkspaceConfigurationOutgoing outputWco = ConfigurationUtil.convertToOutgoing(updatedWc, getSpaceConfigurationId2Name());
            return Response.ok(outputWco).build();
        } catch (Exception e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
    }


    @DELETE
    @Path("/workspaces/{id}")
    public Response deleteWorkspaceConfigurationById(@PathParam("id") String id) {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        boolean deleted = ConfigurationManager.getInstance().removeWorkspaceConfiguration(id);
        if (deleted) {
            return Response.ok().build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/proxy")
    public Response getProxy() {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        ProxyConfigurationOutgoing outgoing = new ProxyConfigurationOutgoing();
        ProxyConfiguration config = ConfigurationManager.getInstance().getProxySettings();
        if (config != null) {
            outgoing.setHost(config.getHost());
            outgoing.setPort(config.getPort() == null ? "" : config.getPort().toString());
            outgoing.setUsername(config.getUsername());

            if (StringUtils.isNotEmpty(config.getPassword())) {
                outgoing.setPassword(PluginConstants.PASSWORD_REPLACE);
            } else {
                outgoing.setPassword("");
            }

            outgoing.setNonProxyHost(config.getNonProxyHost());
        }

        return Response.ok(outgoing).build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/proxy")
    public Response setProxy(final ProxyConfigurationOutgoing proxyOutgoing) {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        Integer port = null;
        if (StringUtils.isNotEmpty(proxyOutgoing.getHost())) {
            try {
                port = Integer.parseInt(proxyOutgoing.getPort());
                if (!(port >= 0 && port <= 65535)) {
                    return Response.status(Status.CONFLICT).entity("Port must range from 0 to 65,535.").build();
                }

            } catch (NumberFormatException e) {
                return Response.status(Status.CONFLICT).entity("Port must be numeric value.").build();
            }
        }

        ConfigurationManager.getInstance().saveProxyConfiguration(proxyOutgoing);
        return Response.ok().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/spaces")
    public Response getSpaceConfigurations() {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        List<SpaceConfigurationOutgoing> outgoing = ConfigurationManager.getInstance().getSpaceConfigurations()
                .stream().map(ConfigurationUtil::convertToOutgoing).collect(Collectors.toList());

        return Response.ok(outgoing).build();
    }

    @POST
    @Path("spaces")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSpaceConfiguration(SpaceConfigurationOutgoing sco) {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        try {
            SpaceConfiguration spaceConfig = ConfigurationUtil.validateRequiredAndConvertToInternal(sco, true);
            ConfigurationUtil.doSpaceConfigurationUniquenessValidation(spaceConfig);
            ConfigurationManager.getInstance().addSpaceConfiguration(spaceConfig);
            return Response.ok(ConfigurationUtil.convertToOutgoing(spaceConfig)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.CONFLICT).entity("Failed to add configuration : " + e.getMessage()).build();
        }
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("spaces/{id}")
    public Response updateSpaceConfiguration(@PathParam("id") String id, final SpaceConfigurationOutgoing sco) {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        try {
            SpaceConfiguration spaceConfig = ConfigurationUtil.validateRequiredAndConvertToInternal(sco, false);
            ConfigurationUtil.doSpaceConfigurationUniquenessValidation(spaceConfig);
            SpaceConfiguration updated = ConfigurationManager.getInstance().updateSpaceConfiguration(spaceConfig);
            return Response.ok(ConfigurationUtil.convertToOutgoing(updated)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.CONFLICT).entity("Failed to update configuration : " + e.getMessage()).build();
        }
    }

    @DELETE
    @Path("spaces/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSpaceConfiguration(@PathParam("id") String id) {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        boolean deleted = ConfigurationManager.getInstance().removeSpaceConfiguration(id);
        if (deleted) {
            return Response.ok().build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @POST
    @Path("spaces/test-connection")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testSpaceConfiguration(SpaceConfigurationOutgoing spaceConfigurationOutgoing) {
        if (!validateUserPermission()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        try {
            boolean isNewConfig = StringUtils.isEmpty(spaceConfigurationOutgoing.getId());
            SpaceConfiguration spaceConfig = ConfigurationUtil.validateRequiredAndConvertToInternal(spaceConfigurationOutgoing, isNewConfig);
            ConfigurationUtil.validateSpaceConfigurationConnectivity(spaceConfig);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
    }

    private boolean validateUserPermission() {
        UserProfile username = userManager.getRemoteUser(request);
        return isAdministrator(userManager, username);
    }

    private Map<String, String> getSpaceConfigurationId2Name() {
        Map<String, String> spaceConfigurationId2Name = ConfigurationManager.getInstance().getSpaceConfigurations().stream()
                .collect(Collectors.toMap(SpaceConfiguration::getId, SpaceConfiguration::getName));
        return spaceConfigurationId2Name;
    }

    public static boolean isAdministrator(UserManager userManager, UserProfile username) {
        return username != null && (userManager.isSystemAdmin(username.getUserKey()) || userManager.isAdmin(username.getUserKey()));
    }
}


