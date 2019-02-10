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

import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.microfocus.octane.plugins.components.api.Constants;
import com.microfocus.octane.plugins.components.api.OctaneRestService;
import com.microfocus.octane.plugins.configuration.ConfigurationCollection;
import com.microfocus.octane.plugins.configuration.LocationParts;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;
import com.microfocus.octane.plugins.configuration.SpaceConfiguration;
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

    @ComponentImport
    private final TransactionTemplate transactionTemplate;

    private final OctaneRestService octaneRestService;

    private List<WorkspaceConfigurationOutgoing> models = new ArrayList<>();

    @Inject
    public ConfigResource(UserManager userManager, TransactionTemplate transactionTemplate, OctaneRestService octaneRestService) {
        this.userManager = userManager;
        this.transactionTemplate = transactionTemplate;
        this.octaneRestService = octaneRestService;

        models.add(new WorkspaceConfigurationOutgoing("1001.1001", "ws1", "key1", Arrays.asList("User story", "Feature"),
                Arrays.asList("JiraIssueType1", "JiraIssueType2", "JiraIssueType3"), Arrays.asList("JiraProject1", "JiraProject2", "JiraProject3")));
        models.add(new WorkspaceConfigurationOutgoing("1001.1002", "ws2", "key2", Collections.emptyList(),
                Collections.emptyList(), Arrays.asList("JiraProject5", "JiraProject6", "JiraProject7")));
    }

    @GET
    @Path("/workspace-config/additional-data/unused-octane-workspaces")
    public Response getUnusedOctaneWorkspace(@Context HttpServletRequest request) {

        Select2Result result = new Select2Result();
        OctaneEntityCollection octaneEntityCollection = octaneRestService.getEntitiesByCondition(OctaneRestService.SPACE_CONTEXT, "workspaces", null, Arrays.asList("id", "name"));
        octaneEntityCollection.getData().forEach(e -> result.addItem(e.getId(), e.getName()));
        return Response.ok(result).build();
    }

    @GET
    @Path("/workspace-config/additional-data")
    public Response getDataForCreateDialog(@Context HttpServletRequest request) {
        OctaneEntityCollection octaneEntityCollection = octaneRestService.getEntitiesByCondition(OctaneRestService.SPACE_CONTEXT, "workspaces", null, Arrays.asList("id", "name"));
        List<Select2ResultItem> workspaces = octaneEntityCollection.getData().stream().map(e -> new Select2ResultItem(e.getId(), e.getName())).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("unusedOctaneWorkspaces", workspaces);
        return Response.ok(data).build();
    }

    @GET
    @Path("/workspace-config/all")
    public Response getAllWorkspaceConfigurations(@Context HttpServletRequest request) {
        return Response.ok(models).build();
    }

    @GET
    @Path("/workspace-config/self/{id}")
    public Response getWorkspaceConfigurationById(@PathParam("id") String id) {
        return Response.ok(models.stream().filter(m -> id.equals("" + m.getId())).findFirst()).build();
    }

    @PUT
    @Path("/workspace-config/self/{id}")
    public Response updateWorkspaceConfigurationById(@Context HttpServletRequest request, @PathParam("id") String id, WorkspaceConfigurationOutgoing modelForUpdate) {
        Optional<WorkspaceConfigurationOutgoing> optional = models.stream().filter(m -> id.equals("" + m.getId())).findFirst();

        return Response.ok(modelForUpdate).build();
    }

    int counter = 3;

    @POST
    @Path("/workspace-config/self")
    public Response createWorkspaceConfiguration(@Context HttpServletRequest request, WorkspaceConfigurationOutgoing model) {
        model.setId("" + counter++);
        models.add(model);
        return Response.ok(model).build();
    }

    @DELETE
    @Path("/workspace-config/self/{id}")
    public Response deleteWorkspaceConfigurationById(@Context HttpServletRequest request, @PathParam("id") String id) {

        Optional<WorkspaceConfigurationOutgoing> optional = models.stream().filter(m -> id.equals("" + m.getId())).findFirst();
        if (optional.isPresent()) {
            models.remove(optional.get());
        }

        return Response.ok().build();
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

        ConfigurationCollection configuration = OctaneConfigurationManager.getInstance().getConfiguration();
        SpaceConfiguration spaceConfig = configuration.getSpaces().get(0);
        SpaceConfigurationOutgoing outgoing = SpaceConfigurationOutgoing
                .create(spaceConfig.getId(), spaceConfig.getLocation(), spaceConfig.getClientId(), OctaneConfigurationManager.PASSWORD_REPLACE);
        return Response.ok(outgoing).build();
    }

    @Path("/test-connection")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response testConnection(final SpaceConfigurationOutgoing spaceModel, @Context HttpServletRequest request) {
        UserProfile username = userManager.getRemoteUser(request);
        if (username == null || !userManager.isSystemAdmin(username.getUserKey())) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        String errorMsg = checkConfiguration(spaceModel);

        if (errorMsg != null) {
            Map<String, String> status = new HashMap<>();
            status.put("failed", "Validation failed : " + errorMsg);
            return Response.status(Status.CONFLICT).entity(status).build();
        }

        return Response.ok().build();
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
                            OctaneConfigurationManager.getInstance().getConfiguration().getSpaces().get(0).getClientSecret() :
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


    private void checkOctaneFieldExistance(List<String> warnings) {
        /*String entityCollectionUrl = String.format(Constants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES,
                internalConfig.getSharedspaceId(), internalConfig.getWorkspaceId(), "metadata/fields");

        Map<String, String> headers = new HashMap<>();
        headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

        try {
            RestConnector restConnector = new RestConnector();
            restConnector.setBaseUrl(internalConfig.getBaseUrl());
            restConnector.setCredentials(internalConfig.getClientId(), internalConfig.getClientSecret());
            boolean isConnected = restConnector.login();

            QueryPhrase fieldNameCondition = new LogicalQueryPhrase("name", internalConfig.getOctaneUdf());
            Map<String, String> key2LabelType = new HashMap<>();
            key2LabelType.put("feature", "Feature");
            key2LabelType.put("story", "User Story");
            key2LabelType.put("product_area", "Application module");
            QueryPhrase typeCondition = new InQueryPhrase("entity_name", key2LabelType.keySet());

            String queryCondition = OctaneQueryBuilder.create().addQueryCondition(fieldNameCondition).addQueryCondition(typeCondition).build();
            String entitiesCollectionStr = restConnector.httpGet(entityCollectionUrl, Arrays.asList(queryCondition), headers).getResponseData();
            OctaneEntityCollection fields = OctaneEntityParser.parseCollection(entitiesCollectionStr);
            Set<String> foundTypes = fields.getData().stream().map(e -> e.getString("entity_name")).collect(Collectors.toSet());
            Set<String> missingTypes = key2LabelType.keySet().stream().filter(key -> !foundTypes.contains(key)).map(key -> key2LabelType.get(key)).collect(Collectors.toSet());
            if (!missingTypes.isEmpty()) {
                String warn = String.format("The following Octane entity types have no field '%s' : %s",
                        internalConfig.getOctaneUdf(), StringUtils.join(missingTypes, ", "));
                warnings.add(warn);
            }
        } catch (Exception e) {
            log.error(String.format("Failed on checkOctaneFieldExistance : %s", e.getMessage()), e);
        }*/
    }


}
